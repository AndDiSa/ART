/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * based on the android tradefederation project
 * modified by AndDiSa 
 */

package de.anddisa.adb.device;

import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.EmulatorConsole;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log.LogLevel;
import de.anddisa.adb.log.LogUtil.CLog;
import de.anddisa.adb.util.ArrayUtil;
import de.anddisa.adb.util.CommandResult;
import de.anddisa.adb.util.CommandStatus;
import de.anddisa.adb.util.ConditionPriorityBlockingQueue;
import de.anddisa.adb.util.ConditionPriorityBlockingQueue.IMatcher;
import de.anddisa.adb.util.IRunUtil;
import de.anddisa.adb.util.RunUtil;
import de.anddisa.adb.util.StreamUtil;
import de.anddisa.adb.util.TableFormatter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@inheritDoc}
 */
public class DeviceManager implements IDeviceManager {

    /** max wait time in ms for fastboot devices command to complete */
    private static final long FASTBOOT_CMD_TIMEOUT = 1 * 60 * 1000;
    /**  time to wait in ms between fastboot devices requests */
    private static final long FASTBOOT_POLL_WAIT_TIME = 5 * 1000;
    /** time to wait for device adb shell responsive connection before declaring it unavailable
     * for testing */
    private static final int CHECK_WAIT_DEVICE_AVAIL_MS = 30 * 1000;

    /** a {@link DeviceSelectionOptions} that matches any device */
    private static final IDeviceSelection ANY_DEVICE_OPTIONS = new DeviceSelectionOptions();

    private static DeviceManager sInstance;

    private boolean mIsInitialized = false;
    /** A thread-safe map that tracks the devices currently allocated for testing.*/
    private Map<String, IManagedTestDevice> mAllocatedDeviceMap;
    /** A FIFO, thread-safe queue for holding devices visible on adb available for testing */
    private ConditionPriorityBlockingQueue<IDevice> mAvailableDeviceQueue;
    private IAndroidDebugBridge mAdbBridge;
    private ManagedDeviceListener mManagedDeviceListener;
    private boolean mFastbootEnabled;
    private Set<IFastbootListener> mFastbootListeners;
    private FastbootMonitor mFastbootMonitor;
    private Map<String, IDeviceStateMonitor> mCheckDeviceMap;
    private boolean mEnableLogcat = true;
    private boolean mIsTerminated = false;
    private IDeviceSelection mGlobalDeviceFilter;
    /** the maximum number of emulators that can be allocated at one time */
    private int mNumEmulatorSupported = 1;
    /** the maximum number of no device runs that can be allocated at one time */
    private int mNumNullDevicesSupported = 1;

    private boolean mSynchronousMode = false;

    /**
     * Package-private constructor, should only be used by this class and its associated unit test.
     * Use {@link #getInstance()} instead.
     */
    DeviceManager() {
    }

    @Override
    public void init(String pathToAdb) {
        init(ANY_DEVICE_OPTIONS, pathToAdb);
    }

    /**
     * Initialize the device manager. This must be called once and only once before any other
     * methods are called.
     */
    @Override
    public synchronized void init(IDeviceSelection globalDeviceFilter, String pathToAdb) {
        if (mIsInitialized) {
            throw new IllegalStateException("already initialized");
        }
        if (pathToAdb == null) {
        	pathToAdb = "adb";
        }
        mIsInitialized = true;
        mGlobalDeviceFilter = globalDeviceFilter;
        // use Hashtable since it is synchronized
        mAllocatedDeviceMap = new Hashtable<String, IManagedTestDevice>();
        mAvailableDeviceQueue = new ConditionPriorityBlockingQueue<IDevice>();
        mCheckDeviceMap = new Hashtable<String, IDeviceStateMonitor>();

        if (isFastbootAvailable()) {
            mFastbootListeners = Collections.synchronizedSet(new HashSet<IFastbootListener>());
            mFastbootMonitor = new FastbootMonitor();
            startFastbootMonitor();
            // don't set fastboot enabled bit until mFastbootListeners has been initialized
            mFastbootEnabled = true;
            // TODO: consider only adding fastboot devices if explicit option is set, because
            // device property selection options won't work properly with a device in fastboot
            addFastbootDevices();
        } else {
            CLog.w("Fastboot is not available.");
            mFastbootListeners = null;
            mFastbootMonitor = null;
            mFastbootEnabled = false;
        }

        // don't start adding devices until fastboot support has been established
        // TODO: Temporarily increase default timeout as workaround for syncFiles timeouts
        DdmPreferences.setTimeOut(30*1000);
        mAdbBridge = createAdbBridge();
        mManagedDeviceListener = new ManagedDeviceListener();
        // It's important to add the listener before initializing the ADB bridge to avoid a race
        // condition when detecting devices.
        mAdbBridge.addDeviceChangeListener(mManagedDeviceListener);
        mAdbBridge.init(false /* client support */, pathToAdb);
        addEmulators();
        addNullDevices();
    }

    /**
     * Instruct DeviceManager whether to use background threads or not.
     * <p/>
     * Exposed to make unit tests more deterministic.
     *
     * @param syncMode
     */
    void setSynchronousMode(boolean syncMode) {
        mSynchronousMode = syncMode;
    }

    private void checkInit() {
        if (!mIsInitialized) {
            throw new IllegalStateException("DeviceManager has not been initialized");
        }
    }

    /**
     * Determine if fastboot is available for use.
     */
    private boolean isFastbootAvailable() {
        CommandResult fastbootResult = getRunUtil().runTimedCmdSilently(5000, "fastboot", "help");
        if (fastbootResult.getStatus() == CommandStatus.SUCCESS) {
            return true;
        }
        if (fastbootResult.getStderr() != null &&
            fastbootResult.getStderr().indexOf("usage: fastboot") >= 0) {
            CLog.logAndDisplay(LogLevel.WARN,
                              "You are running an older version of fastboot, please update it.");
            return true;
        }
        return false;
    }

    /**
     * Start fastboot monitoring.
     * <p/>
     * Exposed for unit testing.
     */
    void startFastbootMonitor() {
        mFastbootMonitor.start();
    }

    /**
     * Get the {@link RunUtil} instance to use.
     * <p/>
     * Exposed for unit testing.
     */
    IRunUtil getRunUtil() {
        return RunUtil.getDefault();
    }

    /**
     * Toggle whether allocated devices should capture logcat in background
     */
    public void setEnableLogcat(boolean enableLogcat) {
        mEnableLogcat = enableLogcat;
    }

    /**
     * Asynchronously checks if device is available, and adds to queue
     * @param device
     */
    private void checkAndAddAvailableDevice(final IDevice device) {
        if (mCheckDeviceMap.containsKey(device.getSerialNumber())) {
            // device already being checked, ignore
            CLog.d("Already checking new device %s, ignoring", device.getSerialNumber());
            return;
        }
        if (!mGlobalDeviceFilter.matches(device)) {
            CLog.v("New device %s doesn't match global filter, ignoring", device.getSerialNumber());
            return;
        }
        final IDeviceStateMonitor monitor = createStateMonitor(device);
        mCheckDeviceMap.put(device.getSerialNumber(), monitor);

        final String threadName = String.format("Check device %s", device.getSerialNumber());
        Runnable checkRunnable = new Runnable() {
            @Override
            public void run() {
                CLog.d("checking new device %s responsiveness", device.getSerialNumber());
                if (monitor.waitForDeviceShell(CHECK_WAIT_DEVICE_AVAIL_MS)) {
                    CLog.logAndDisplay(LogLevel.INFO, "Detected new device %s",
                            device.getSerialNumber());
                    addAvailableDevice(device);
                } else{
                    CLog.e("Device %s is not responsive to adb shell command , " +
                            "skip adding to available pool", device.getSerialNumber());
                }
                mCheckDeviceMap.remove(device.getSerialNumber());
            }
        };
        if (mSynchronousMode ) {
            checkRunnable.run();
        } else {
            Thread checkThread = new Thread(checkRunnable, threadName);
            // Device checking threads shouldn't hold the JVM open
            checkThread.setDaemon(true);
            checkThread.start();
        }
    }

    /**
     * Add placeholder objects for the max number of 'no device required' concurrent allocations
     */
    private void addNullDevices() {
        for (int i = 0; i < mNumNullDevicesSupported; i++) {
            addAvailableDevice(new NullDevice(String.format("null-device-%d", i)));
        }
    }

    /**
     * Add placeholder objects for the max number of emulators that can be allocated
     */
    private void addEmulators() {
        // TODO currently this means 'additional emulators not already running'
        int port = 5554;
        for (int i = 0; i < mNumEmulatorSupported; i++) {
            addAvailableDevice(new StubDevice(String.format("emulator-%d", port), true));
            port += 2;
        }
    }

    private void addFastbootDevices() {
        Set<String> serials = getDevicesOnFastboot();
        if (serials != null) {
            for (String serial: serials) {
                addAvailableDevice(new FastbootDevice(serial));
            }
        }
    }

    private static class FastbootDevice extends StubDevice {
        FastbootDevice(String serial) {
            super(serial, false);
        }
    }

    /**
     * Creates a {@link IDeviceStateMonitor} to use.
     * <p/>
     * Exposed so unit tests can mock
     */
    IDeviceStateMonitor createStateMonitor(IDevice device) {
        return new DeviceStateMonitor(this, device, mFastbootEnabled);
    }

    private void addAvailableDevice(final IDevice device) {
        IMatcher<IDevice> deviceSerialMatcher = new IMatcher<IDevice>() {
            @Override
            public boolean matches(IDevice element) {
                return element.getSerialNumber().equals(device.getSerialNumber());
            }

        };
        // add IDevice to available queue, replacing any existing IDevice with same serial
        IDevice existingObject = mAvailableDeviceQueue.addUnique(deviceSerialMatcher, device);
        if (existingObject != null) {
            // TODO: reduce severity level for this log. Leaving high for now to understand
            // circumstances where this can happen
            CLog.w("Found existing device for available device %s", device.getSerialNumber());
        }
    }

    /**
     * Get the available device queue.
     * <p/>
     * Exposed for unit testing
     * @return
     */
    ConditionPriorityBlockingQueue<IDevice> getAvailableDeviceQueue() {
        return mAvailableDeviceQueue;
    }

    /**
     * Return the {@link IDeviceManager} singleton, creating if necessary.
     */
    public synchronized static IDeviceManager getInstance() {
        if (sInstance == null) {
            sInstance = new DeviceManager();
        }
        return sInstance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITestDevice allocateDevice() {
        checkInit();
        IDevice allocatedDevice = takeAvailableDevice();
        if (allocatedDevice == null) {
            return null;
        }
        return createAllocatedDevice(allocatedDevice);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITestDevice forceAllocateDevice(String serial) {
        checkInit();
        if (mAllocatedDeviceMap.containsKey(serial)) {
            CLog.w("Device %s is already allocated", serial);
            return null;
        }
        // first try to allocate that device as normal
        DeviceSelectionOptions options = new DeviceSelectionOptions();
        options.addSerial(serial);
        IDevice allocatedDevice = pollAvailableDevice(1, options);
        if (allocatedDevice == null) {
            // not there? allocate a stub device
            allocatedDevice = new StubDevice(serial, false);
        }
        return createAllocatedDevice(allocatedDevice);
    }

    /**
     * Retrieves and removes a IDevice from the available device queue, waiting indefinitely if
     * necessary until an IDevice becomes available.
     *
     * @return the {@link IDevice} or <code>null</code> if interrupted
     */
    private IDevice takeAvailableDevice() {
        try {
            return mAvailableDeviceQueue.take(ANY_DEVICE_OPTIONS);
        } catch (InterruptedException e) {
            CLog.w("interrupted while taking device");
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITestDevice allocateDevice(long timeout) {
        checkInit();
        IDevice allocatedDevice = pollAvailableDevice(timeout, ANY_DEVICE_OPTIONS);
        if (allocatedDevice == null) {
            return null;
        }
        return createAllocatedDevice(allocatedDevice);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITestDevice allocateDevice(long timeout, IDeviceSelection options) {
        checkInit();
        IDevice allocatedDevice = pollAvailableDevice(timeout, options);
        if (allocatedDevice == null) {
            return null;
        }
        return createAllocatedDevice(allocatedDevice);
    }

    /**
     * Retrieves and removes a IDevice from the available device queue, waiting for timeout if
     * necessary until an IDevice becomes available.
     *
     * @param timeout the number of ms to wait for device
     * @param options the {@link DeviceSelectionOptions} the returned device must meet
     *
     * @return the {@link IDevice} or <code>null</code> if interrupted
     */
    private IDevice pollAvailableDevice(long timeout, IDeviceSelection options) {
        try {
            return mAvailableDeviceQueue.poll(timeout, TimeUnit.MILLISECONDS, options);
        } catch (InterruptedException e) {
            CLog.w("interrupted while polling for device");
            return null;
        }
    }

    private ITestDevice createAllocatedDevice(IDevice allocatedDevice) {
        IManagedTestDevice testDevice = createTestDevice(allocatedDevice,
                createStateMonitor(allocatedDevice));
        if (mEnableLogcat && !(allocatedDevice instanceof StubDevice)) {
            testDevice.startLogcat();
        }
        mAllocatedDeviceMap.put(allocatedDevice.getSerialNumber(), testDevice);
        CLog.i("Allocated device %s", testDevice.getSerialNumber());
        return testDevice;
    }

    /**
     * Factory method to create a {@link IManagedTestDevice}.
     * <p/>
     * Exposed so unit tests can mock
     *
     * @param allocatedDevice
     * @param monitor
     * @return a {@link IManagedTestDevice}
     */
    IManagedTestDevice createTestDevice(IDevice allocatedDevice, IDeviceStateMonitor monitor) {
        IManagedTestDevice testDevice = new TestDevice(allocatedDevice, monitor);
        testDevice.setFastbootEnabled(mFastbootEnabled);
        if (allocatedDevice instanceof FastbootDevice) {
            testDevice.setDeviceState(TestDeviceState.FASTBOOT);
        } else if (allocatedDevice instanceof StubDevice) {
            testDevice.setDeviceState(TestDeviceState.NOT_AVAILABLE);
        }
        return testDevice;
    }

    /**
     * Creates the {@link IAndroidDebugBridge} to use.
     * <p/>
     * Exposed so tests can mock this.
     * @returns the {@link IAndroidDebugBridge}
     */
    synchronized IAndroidDebugBridge createAdbBridge() {
        return new AndroidDebugBridgeWrapper();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void freeDevice(ITestDevice device, FreeDeviceState deviceState) {
        checkInit();
        IManagedTestDevice managedDevice = (IManagedTestDevice)device;
        managedDevice.stopLogcat();
        IDevice ideviceToReturn = device.getIDevice();
        // don't kill emulator if it wasn't launched by launchEmulator (ie emulatorProcess is null).
        if (ideviceToReturn.isEmulator() && managedDevice.getEmulatorProcess() != null) {
            try {
                killEmulator(device);
                // emulator killed - return a stub device
                // TODO: this is a bit of a hack. Consider having DeviceManager inject a StubDevice
                // when deviceDisconnected event is received
                ideviceToReturn = new StubDevice(ideviceToReturn.getSerialNumber(), true);
                deviceState = FreeDeviceState.AVAILABLE;
            } catch (DeviceNotAvailableException e) {
                CLog.e(e);
                deviceState = FreeDeviceState.UNAVAILABLE;
            }
        }
        if (mAllocatedDeviceMap.remove(device.getSerialNumber()) == null) {
            CLog.e("freeDevice called with unallocated device %s",
                    device.getSerialNumber());
        } else if (deviceState == FreeDeviceState.UNRESPONSIVE) {
            // TODO: add class flag to control if unresponsive device's are returned to pool
            // TODO: also consider tracking unresponsive events received per device - so a
            // device that is continually unresponsive could be removed from available queue
            addAvailableDevice(ideviceToReturn);
        } else if (deviceState == FreeDeviceState.AVAILABLE) {
            addAvailableDevice(ideviceToReturn);
        } else if (deviceState == FreeDeviceState.UNAVAILABLE) {
            CLog.logAndDisplay(LogLevel.WARN, "Freed device %s is unavailable. Removing from use.",
                    device.getSerialNumber());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void launchEmulator(ITestDevice device, long bootTimeout, IRunUtil runUtil,
            List<String> emulatorArgs)
            throws DeviceNotAvailableException {
        if (!device.getIDevice().isEmulator()) {
            throw new IllegalStateException(String.format("Device %s is not an emulator",
                    device.getSerialNumber()));
        }
        if (!device.getDeviceState().equals(TestDeviceState.NOT_AVAILABLE)) {
            throw new IllegalStateException(String.format(
                    "Emulator device %s is in state %s. Expected: %s", device.getSerialNumber(),
                    device.getDeviceState(), TestDeviceState.NOT_AVAILABLE));
        }
        Integer port = EmulatorConsole.getEmulatorPort(device.getSerialNumber());
        if (port == null) {
            // serial number is not in expected format
            throw new IllegalArgumentException(String.format(
                    "Failed to determine emulator port for %s", device.getSerialNumber()));
        }
        List<String> fullArgs = new ArrayList<String>(emulatorArgs);
        fullArgs.add("-port");
        fullArgs.add(port.toString());

        try {
            Process p = runUtil.runCmdInBackground(fullArgs);
            // sleep a small amount to wait for process to start successfully
            getRunUtil().sleep(500);
            checkProcessDied(p);
            IManagedTestDevice managedDevice = (IManagedTestDevice)device;
            managedDevice.setEmulatorProcess(p);
            managedDevice.startLogcat();
        } catch (IOException e) {
            // TODO: is this the most appropriate exception to throw?
            throw new DeviceNotAvailableException("Failed to start emulator process", e);
        }

        device.waitForDeviceAvailable(bootTimeout);
    }

    /**
     * Check if emulator process has died
     *
     * @param p the {@link Process} to check
     * @throws DeviceNotAvailableException if process has died
     */
    private void checkProcessDied(Process p) throws DeviceNotAvailableException {
        try {
            int exitValue = p.exitValue();
            // should have thrown IllegalThreadStateException
            CLog.e("Emulator process has died with exit value %d. stdout: '%s', stderr: '%s'",
                    exitValue, StreamUtil.getStringFromStream(p.getInputStream()),
                    StreamUtil.getStringFromStream(p.getErrorStream()));
        } catch (IllegalThreadStateException e) {
            // expected if process is still alive
            return;
        } catch (IOException e) {
            // fall through
        }
        throw new DeviceNotAvailableException("Emulator process has died unexpectedly");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void killEmulator(ITestDevice device) throws DeviceNotAvailableException {
        EmulatorConsole console = EmulatorConsole.getConsole(device.getIDevice());
        if (console != null) {
            console.kill();
            // lets ensure process is killed too - fall through
        } else {
            CLog.w("Could not get emulator console for %s", device.getSerialNumber());
        }
        // lets try killing the process
        Process emulatorProcess = ((IManagedTestDevice)device).getEmulatorProcess();
        if (emulatorProcess != null) {
            emulatorProcess.destroy();
        }
        if (!device.waitForDeviceNotAvailable(20*1000)) {
            throw new DeviceNotAvailableException(String.format("Failed to kill emulator %s",
                    device.getSerialNumber()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITestDevice connectToTcpDevice(String ipAndPort) {
        if (mAllocatedDeviceMap.containsKey(ipAndPort)) {
            CLog.w("Device with tcp serial %s is already allocated", ipAndPort);
            return null;
        }
        // create a mapping between this device, and its soon-to-be associated tcp serial number
        // this is done so a) the device can get state updates and b) this device isn't allocated
        // to another caller when it goes online with new serial
        ITestDevice tcpDevice = createAllocatedDevice(new StubDevice(ipAndPort));
        if (doAdbConnect(ipAndPort)) {
            try {
                tcpDevice.setRecovery(new WaitDeviceRecovery());
                tcpDevice.waitForDeviceOnline();
                return tcpDevice;
            } catch (DeviceNotAvailableException e) {
                CLog.w("Device with tcp serial %s did not come online", ipAndPort);
            }
        }
        freeDevice(tcpDevice, FreeDeviceState.IGNORE);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITestDevice reconnectDeviceToTcp(ITestDevice usbDevice)
            throws DeviceNotAvailableException {
        CLog.i("Reconnecting device %s to adb over tcpip", usbDevice.getSerialNumber());
        ITestDevice tcpDevice = null;
        if (usbDevice instanceof IManagedTestDevice) {
            IManagedTestDevice managedUsbDevice = (IManagedTestDevice)usbDevice;
            String ipAndPort = managedUsbDevice.switchToAdbTcp();
            if (ipAndPort != null) {
                CLog.d("Device %s was switched to adb tcp on %s", usbDevice.getSerialNumber(),
                        ipAndPort);
                tcpDevice = connectToTcpDevice(ipAndPort);
                if (tcpDevice == null) {
                    // ruh roh, could not connect to device
                    // Try to re-establish connection back to usb device
                    managedUsbDevice.recoverDevice();
                }
            }
        } else {
            CLog.e("reconnectDeviceToTcp: unrecognized device type.");
        }
        return tcpDevice;
    }

    @Override
    public boolean disconnectFromTcpDevice(ITestDevice tcpDevice) {
        CLog.i("Disconnecting and freeing tcp device %s", tcpDevice.getSerialNumber());
        boolean result = false;
        try {
            result = tcpDevice.switchToAdbUsb();
        } catch (DeviceNotAvailableException e) {
            CLog.w("Failed to switch device %s to usb mode: %s", tcpDevice.getSerialNumber(),
                    e.getMessage());
        }
        freeDevice(tcpDevice, FreeDeviceState.IGNORE);
        return result;
    }

    private boolean doAdbConnect(String ipAndPort) {
        final String resultSuccess = String.format("connected to %s", ipAndPort);
        for (int i = 1; i <= 3; i++) {
            String adbConnectResult = executeGlobalAdbCommand("connect", ipAndPort);
            // runcommand "adb connect ipAndPort"
            if (adbConnectResult.startsWith(resultSuccess)) {
                return true;
            }
            CLog.w("Failed to connect to device on %s, attempt %d of 3. Response: %s.",
                    ipAndPort, i, adbConnectResult);
            getRunUtil().sleep(5*1000);
        }
        return false;
    }

    /**
     * Execute a adb command not targeted to a particular device eg. 'adb connect'
     *
     * @param cmdArgs
     * @return
     */
    public String executeGlobalAdbCommand(String... cmdArgs) {
        String[] fullCmd = ArrayUtil.buildArray(new String[] {"adb"}, cmdArgs);
        CommandResult result = getRunUtil().runTimedCmd(FASTBOOT_CMD_TIMEOUT, fullCmd);
        if (CommandStatus.SUCCESS.equals(result.getStatus())) {
            return result.getStdout();
        }
        CLog.w("adb %s failed", cmdArgs[0]);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void terminate() {
        checkInit();
        if (!mIsTerminated ) {
            mIsTerminated = true;
            mAdbBridge.removeDeviceChangeListener(mManagedDeviceListener);
            mAdbBridge.terminate();
            if (mFastbootMonitor != null) {
                mFastbootMonitor.terminate();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void terminateHard() {
        checkInit();
        if (!mIsTerminated ) {
            for (IManagedTestDevice device : mAllocatedDeviceMap.values()) {
                device.setRecovery(new AbortRecovery());
            }
            mAdbBridge.disconnectBridge();
            terminate();
        }
    }

    private static class AbortRecovery implements IDeviceRecovery {

        /**
         * {@inheritDoc}
         */
        @Override
        public void recoverDevice(IDeviceStateMonitor monitor, boolean recoverUntilOnline)
                throws DeviceNotAvailableException {
            throw new DeviceNotAvailableException("aborted test session");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void recoverDeviceBootloader(IDeviceStateMonitor monitor)
                throws DeviceNotAvailableException {
            throw new DeviceNotAvailableException("aborted test session");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void recoverDeviceRecovery(IDeviceStateMonitor monitor)
                throws DeviceNotAvailableException {
            throw new DeviceNotAvailableException("aborted test session");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Collection<String> getAllocatedDevices() {
        checkInit();
        Collection<String> allocatedDeviceSerials = new ArrayList<String>(
                mAllocatedDeviceMap.size());
        allocatedDeviceSerials.addAll(mAllocatedDeviceMap.keySet());
        return allocatedDeviceSerials;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Collection<String> getAvailableDevices() {
        checkInit();
        Collection<String> availableDeviceSerials = new ArrayList<String>(
                mAvailableDeviceQueue.size());
        synchronized (mAvailableDeviceQueue) {
            for (IDevice device : mAvailableDeviceQueue) {
                // don't add placeholder devices to available devices display
                if (!(device instanceof StubDevice)) {
                    availableDeviceSerials.add(device.getSerialNumber());
                }
            }
        }
        return availableDeviceSerials;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Collection<String> getUnavailableDevices() {
        checkInit();
        IDevice[] visibleDevices = mAdbBridge.getDevices();
        Collection<String> unavailableSerials = new ArrayList<String>(
                visibleDevices.length);
        Collection<String> availSerials = getAvailableDevices();
        Collection<String> allocatedSerials = getAllocatedDevices();
        for (IDevice device : visibleDevices) {
            if (!availSerials.contains(device.getSerialNumber()) &&
                    !allocatedSerials.contains(device.getSerialNumber())) {
                unavailableSerials.add(device.getSerialNumber());
            }
        }
        return unavailableSerials;
    }

    @Override
    public void displayDevicesInfo(PrintWriter stream) {
        Map<IDevice, String> deviceMap = new LinkedHashMap<IDevice, String>();
        ArrayList<List<String>> displayRows = new ArrayList<List<String>>();
        displayRows.add(Arrays.asList("Serial", "State", "Product", "Variant", "Build",
                "Battery"));
        synchronized (this) {
            checkInit();
            Set<IDevice> visibleDeviceSet = new HashSet<IDevice>();
            Collections.addAll(visibleDeviceSet, mAdbBridge.getDevices());

            for (ITestDevice device : mAllocatedDeviceMap.values()) {
                deviceMap.put(device.getIDevice(), "Allocated");
                visibleDeviceSet.remove(device.getIDevice());
            }

            for (IDevice device : mAvailableDeviceQueue) {
                // don't add placeholder devices to available devices display
                if (!(device instanceof StubDevice)) {
                    deviceMap.put(device, "Available");
                    visibleDeviceSet.remove(device);
                }
            }

            for (IDevice device : visibleDeviceSet) {
                deviceMap.put(device, "Unavailable");
            }
        }
        IDeviceSelection selector = getDeviceSelectionOptions();
        addDevicesInfo(selector, displayRows, deviceMap);
        new TableFormatter().displayTable(displayRows, stream);
    }

    /**
     * Get the {@link IDeviceSelection} to use to display device info
     * <p/>
     * Exposed for unit testing.
     */
    IDeviceSelection getDeviceSelectionOptions() {
        return new DeviceSelectionOptions();
    }

    private void addDevicesInfo(IDeviceSelection selector, List<List<String>> displayRows,
            Map<IDevice, String> deviceStateMap) {
        for (Map.Entry<IDevice, String> deviceEntry : deviceStateMap.entrySet()) {
            IDevice device = deviceEntry.getKey();
            String deviceState = deviceEntry.getValue();
            displayRows.add(Arrays.asList(
                    device.getSerialNumber(),
                    deviceState,
                    getDisplay(selector.getDeviceProductType(device)),
                    getDisplay(selector.getDeviceProductVariant(device)),
                    getDisplay(device.getProperty("ro.build.id")),
                    getDisplay(selector.getBatteryLevel(device)))
            );
        }
    }

    /**
     * Gets a displayable string for given object
     * @param o
     * @return
     */
    private String getDisplay(Object o) {
        return o == null ? "unknown" : o.toString();
    }

    /**
     * A class to listen for and act on device presence updates from ddmlib
     */
    private class ManagedDeviceListener implements IDeviceChangeListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void deviceChanged(IDevice device, int changeMask) {
            IManagedTestDevice testDevice = mAllocatedDeviceMap.get(device.getSerialNumber());
            if ((changeMask & IDevice.CHANGE_STATE) != 0) {
                if (testDevice != null) {
                    TestDeviceState newState = TestDeviceState.getStateByDdms(device.getState());
                    testDevice.setDeviceState(newState);
                } else if (mCheckDeviceMap.containsKey(device.getSerialNumber())) {
                    IDeviceStateMonitor monitor = mCheckDeviceMap.get(device.getSerialNumber());
                    monitor.setState(TestDeviceState.getStateByDdms(device.getState()));
                } else if (!mAvailableDeviceQueue.contains(device) && validAdbState(device.getState())) {
                    checkAndAddAvailableDevice(device);
                }
            }
        }

        private boolean validAdbState(IDevice.DeviceState deviceState) {
        	return (deviceState == IDevice.DeviceState.ONLINE || deviceState == IDevice.DeviceState.RECOVERY);
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public void deviceConnected(IDevice device) {
            CLog.d("Detected device connect %s, id %d", device.getSerialNumber(),
                    device.hashCode());
            IManagedTestDevice testDevice = mAllocatedDeviceMap.get(device.getSerialNumber());
            if (testDevice == null) {
                if (isValidDeviceSerial(device.getSerialNumber()) && validAdbState(device.getState())) {
                    checkAndAddAvailableDevice(device);
                } else if (mCheckDeviceMap.containsKey(device.getSerialNumber())) {
                    IDeviceStateMonitor monitor = mCheckDeviceMap.get(device.getSerialNumber());
                    monitor.setState(TestDeviceState.getStateByDdms(device.getState()));
                }
            } else {
                // this device is known already. However DDMS will allocate a new IDevice, so need
                // to update the TestDevice record with the new device
                CLog.d("Updating IDevice for device %s", device.getSerialNumber());
                testDevice.setIDevice(device);
                TestDeviceState newState = TestDeviceState.getStateByDdms(device.getState());
                testDevice.setDeviceState(newState);
            }
        }

        private boolean isValidDeviceSerial(String serial) {
            return serial.length() > 1 && !serial.contains("?");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void deviceDisconnected(IDevice disconnectedDevice) {
            if (mAvailableDeviceQueue.remove(disconnectedDevice)) {
                CLog.i("Removed disconnected device %s from available queue",
                        disconnectedDevice.getSerialNumber());
            }
            IManagedTestDevice testDevice = mAllocatedDeviceMap.get(
                    disconnectedDevice.getSerialNumber());
            if (testDevice != null) {
                testDevice.setDeviceState(TestDeviceState.NOT_AVAILABLE);
            } else if (mCheckDeviceMap.containsKey(disconnectedDevice.getSerialNumber())) {
                IDeviceStateMonitor monitor = mCheckDeviceMap.get(
                        disconnectedDevice.getSerialNumber());
                monitor.setState(TestDeviceState.NOT_AVAILABLE);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFastbootListener(IFastbootListener listener) {
        checkInit();
        if (mFastbootEnabled) {
            mFastbootListeners.add(listener);
        } else {
            throw new UnsupportedOperationException("fastboot is not enabled");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFastbootListener(IFastbootListener listener) {
        checkInit();
        if (mFastbootEnabled) {
            mFastbootListeners.remove(listener);
        }
    }

    private class FastbootMonitor extends Thread {

        private boolean mQuit = false;

        FastbootMonitor() {
            super("FastbootMonitor");
        }

        public void terminate() {
            mQuit = true;
            interrupt();
        }

        @Override
        public void run() {
            while (!mQuit) {
                // only poll fastboot devices if there are listeners, as polling it
                // indiscriminately can cause fastboot commands to hang
                if (!mFastbootListeners.isEmpty()) {
                    Set<String> serials = getDevicesOnFastboot();
                    if (serials != null) {
                        for (String serial: serials) {
                            IManagedTestDevice testDevice = mAllocatedDeviceMap.get(serial);
                            if (testDevice != null &&
                                    !testDevice.getDeviceState().equals(TestDeviceState.FASTBOOT)) {
                                testDevice.setDeviceState(TestDeviceState.FASTBOOT);
                            }
                        }
                        // now update devices that are no longer on fastboot
                        synchronized (mAllocatedDeviceMap) {
                            for (IManagedTestDevice testDevice : mAllocatedDeviceMap.values()) {
                                if (!serials.contains(testDevice.getSerialNumber())
                                        && testDevice.getDeviceState().equals(
                                                TestDeviceState.FASTBOOT)) {
                                    testDevice.setDeviceState(TestDeviceState.NOT_AVAILABLE);
                                }
                            }
                        }
                        // create a copy of listeners for notification to prevent deadlocks
                        Collection<IFastbootListener> listenersCopy = new ArrayList<IFastbootListener>(
                                mFastbootListeners.size());
                        listenersCopy.addAll(mFastbootListeners);
                        for (IFastbootListener listener : listenersCopy) {
                            listener.stateUpdated();
                        }
                    }
                }
                getRunUtil().sleep(FASTBOOT_POLL_WAIT_TIME);
            }
        }
    }

    private Set<String> getDevicesOnFastboot() {
        CommandResult fastbootResult = getRunUtil().runTimedCmd(FASTBOOT_CMD_TIMEOUT,
                "fastboot", "devices");
        if (fastbootResult.getStatus().equals(CommandStatus.SUCCESS)) {
            CLog.v("fastboot devices returned\n %s",
                    fastbootResult.getStdout());
            return parseDevicesOnFastboot(fastbootResult.getStdout());
        } else {
            CLog.w("'fastboot devices' failed. Result: %s, stderr: %s", fastbootResult.getStatus(),
                    fastbootResult.getStderr());
        }
        return null;
    }

    static Set<String> parseDevicesOnFastboot(String fastbootOutput) {
        Set<String> serials = new HashSet<String>();
        Pattern fastbootPattern = Pattern.compile("([\\w\\d]+)\\s+fastboot\\s*");
        Matcher fastbootMatcher = fastbootPattern.matcher(fastbootOutput);
        while (fastbootMatcher.find()) {
            serials.add(fastbootMatcher.group(1));
        }
        return serials;
    }
}
