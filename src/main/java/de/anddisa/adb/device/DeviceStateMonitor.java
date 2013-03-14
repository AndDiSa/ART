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
 */
package de.anddisa.adb.device;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.CollectingOutputReceiver;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import de.anddisa.adb.device.IDeviceManager.IFastbootListener;
import de.anddisa.adb.log.LogUtil.CLog;
import de.anddisa.adb.util.IRunUtil;
import de.anddisa.adb.util.RunUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Helper class for monitoring the state of a {@link IDevice}.
 */
class DeviceStateMonitor implements IDeviceStateMonitor {

    private static final String LOG_TAG = "DeviceStateMonitor";
    private IDevice mDevice;
    private TestDeviceState mDeviceState;

    /** the time in ms to wait between 'poll for responsiveness' attempts */
    private static final long CHECK_POLL_TIME = 3 * 1000;
    /** the maximum operation time in ms for a 'poll for responsiveness' command */
    private static final int MAX_OP_TIME = 10 * 1000;

    /** The  time in ms to wait for a device to be online. */
    private long mDefaultOnlineTimeout = 1 * 60 * 1000;

    /** The  time in ms to wait for a device to available. */
    private long mDefaultAvailableTimeout = 6 * 60 * 1000;

    private List<DeviceStateListener> mStateListeners;
    private IDeviceManager mMgr;
    private final boolean mFastbootEnabled;

    DeviceStateMonitor(IDeviceManager mgr, IDevice device, boolean fastbootEnabled) {
        mMgr = mgr;
        mDevice = device;
        mStateListeners = new ArrayList<DeviceStateListener>();
        mDeviceState = TestDeviceState.getStateByDdms(device.getState());
        mFastbootEnabled = fastbootEnabled;
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
     * Set the time in ms to wait for a device to be online in {@link #waitForDeviceOnline()}.
     */
    @Override
    public void setDefaultOnlineTimeout(long timeoutMs) {
        mDefaultOnlineTimeout = timeoutMs;
    }

    /**
     * Set the time in ms to wait for a device to be available in {@link #waitForDeviceAvailable()}.
     */
    @Override
    public void setDefaultAvailableTimeout(long timeoutMs) {
        mDefaultAvailableTimeout = timeoutMs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDevice waitForDeviceOnline(long waitTime) {
        if (waitForDeviceState(TestDeviceState.ONLINE, waitTime)) {
            return getIDevice();
        }
        return null;
    }

    /**
     * @return
     */
    private IDevice getIDevice() {
        synchronized (mDevice) {
            return mDevice;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSerialNumber() {
        return getIDevice().getSerialNumber();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDevice waitForDeviceOnline() {
        return waitForDeviceOnline(mDefaultOnlineTimeout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean waitForDeviceNotAvailable(long waitTime) {
        IFastbootListener listener = new StubFastbootListener();
        if (mFastbootEnabled) {
            mMgr.addFastbootListener(listener);
        }
        boolean result = waitForDeviceState(TestDeviceState.NOT_AVAILABLE, waitTime);
        if (mFastbootEnabled) {
            mMgr.removeFastbootListener(listener);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean waitForDeviceInRecovery(long waitTime) {
        return waitForDeviceState(TestDeviceState.RECOVERY, waitTime);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean waitForDeviceShell(final long waitTime) {
        CLog.i("Waiting %d ms for device %s shell to be responsive", waitTime,
                getSerialNumber());
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < waitTime) {
            final CollectingOutputReceiver receiver = new CollectingOutputReceiver();
            final String cmd = "ls";
            try {
                getIDevice().executeShellCommand(cmd, receiver, MAX_OP_TIME);
                String output = receiver.getOutput();
                if (output.contains("system")) {
                    return true;
                }
            } catch (IOException e) {
                CLog.i("%s failed: %s", cmd, e.getMessage());
            } catch (TimeoutException e) {
                CLog.i("%s failed: timeout", cmd);
            } catch (AdbCommandRejectedException e) {
                CLog.i("%s failed: %s", cmd, e.getMessage());
            } catch (ShellCommandUnresponsiveException e) {
                CLog.i("%s failed: %s", cmd, e.getMessage());
            }
            getRunUtil().sleep(CHECK_POLL_TIME);
        }
        CLog.w("Device %s shell is unresponsive", getSerialNumber());
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDevice waitForDeviceAvailable(final long waitTime) {
        // A device is currently considered "available" if and only if four events are true:
        // 1. Device is online aka visible via DDMS/adb
        // 2. Device has dev.bootcomplete flag set
        // 3. Device's package manager is responsive
        // 4. Device's external storage is mounted
        //
        // The current implementation waits for each event to occur in sequence.
        //
        // it will track the currently elapsed time and fail if it is
        // greater than waitTime

        long startTime = System.currentTimeMillis();
        IDevice device = waitForDeviceOnline(waitTime);
        if (device == null) {
            return null;
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (!waitForBootComplete(waitTime - elapsedTime)) {
            return null;
        }
        elapsedTime = System.currentTimeMillis() - startTime;
        if (!waitForPmResponsive(waitTime - elapsedTime)) {
            return null;
        }
        elapsedTime = System.currentTimeMillis() - startTime;
        if (!waitForStoreMount(waitTime - elapsedTime)) {
            return null;
        }
        return device;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDevice waitForDeviceAvailable() {
        return waitForDeviceAvailable(mDefaultAvailableTimeout);
    }

    /**
     * Blocks until the device's boot complete flag is set
     *
     * @param waitTime the amount in ms to wait
     */
    private boolean waitForBootComplete(final long waitTime) {
        CLog.i("Waiting %d ms for device %s boot complete", waitTime, getSerialNumber());
        long startTime = System.currentTimeMillis();
        final String cmd = "getprop dev.bootcomplete";
        while ((System.currentTimeMillis() - startTime) < waitTime) {
            try {
                String bootFlag = getIDevice().getPropertySync("dev.bootcomplete");
                if ("1".equals(bootFlag)) {
                    return true;
                }
            } catch (IOException e) {
                CLog.i("%s failed %s", cmd, e.getMessage());
            } catch (TimeoutException e) {
                CLog.i("%s failed: timeout", cmd);
            } catch (AdbCommandRejectedException e) {
                CLog.i("%s failed: %s", cmd, e.getMessage());
            } catch (ShellCommandUnresponsiveException e) {
                CLog.i("%s failed: %s", cmd, e.getMessage());
            }
            getRunUtil().sleep(CHECK_POLL_TIME);
        }
        CLog.w("Device %s did not boot after %d ms", getSerialNumber(), waitTime);
        return false;
    }

    /**
     * Waits for the device package manager to be responsive.
     *
     * @param waitTime time in ms to wait before giving up
     * @return <code>true</code> if package manage becomes responsive before waitTime expires.
     * <code>false</code> otherwise
     */
    private boolean waitForPmResponsive(final long waitTime) {
        CLog.i("Waiting %d ms for device %s package manager",
                waitTime, getSerialNumber());
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < waitTime) {
            final CollectingOutputReceiver receiver = new CollectingOutputReceiver();
            final String cmd = "pm path android";
            try {
                getIDevice().executeShellCommand(cmd, receiver, MAX_OP_TIME);
                String output = receiver.getOutput();
                Log.v(LOG_TAG, String.format("%s returned %s", cmd, output));
                if (output.contains("package:")) {
                    return true;
                }
            } catch (IOException e) {
                Log.i(LOG_TAG, String.format("%s failed: %s", cmd, e.getMessage()));
            } catch (TimeoutException e) {
                Log.i(LOG_TAG, String.format("%s failed: timeout", cmd));
            } catch (AdbCommandRejectedException e) {
                Log.i(LOG_TAG, String.format("%s failed: %s", cmd, e.getMessage()));
            } catch (ShellCommandUnresponsiveException e) {
                Log.i(LOG_TAG, String.format("%s failed: %s", cmd, e.getMessage()));
            }
            getRunUtil().sleep(CHECK_POLL_TIME);
        }
        Log.w(LOG_TAG, String.format("Device %s package manager is unresponsive",
                getSerialNumber()));
        return false;
    }

    /**
     * Waits for the device's external store to be mounted.
     *
     * @param waitTime time in ms to wait before giving up
     * @return <code>true</code> if external store is mounted before waitTime expires.
     * <code>false</code> otherwise
     */
    private boolean waitForStoreMount(final long waitTime) {
        Log.i(LOG_TAG, String.format("Waiting %d ms for device %s external store", waitTime,
                getSerialNumber()));
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < waitTime) {
            final CollectingOutputReceiver receiver = new CollectingOutputReceiver();
            final CollectingOutputReceiver bitBucket = new CollectingOutputReceiver();
            final long number = System.currentTimeMillis();
            final String externalStore = getMountPoint(IDevice.MNT_EXTERNAL_STORAGE);

            final String testFile = String.format("'%s/%d'", externalStore, number);
            final String testString = String.format("number %d one", number);
            final String writeCmd = String.format("echo '%s' > %s", testString, testFile);
            final String checkCmd = String.format("cat %s", testFile);
            final String cleanupCmd = String.format("rm %s", testFile);
            String cmd = null;
            if (externalStore != null) {
                try {
                    cmd = writeCmd;
                    getIDevice().executeShellCommand(writeCmd, bitBucket, MAX_OP_TIME);
                    cmd = checkCmd;
                    getIDevice().executeShellCommand(checkCmd, receiver, MAX_OP_TIME);
                    cmd = cleanupCmd;
                    getIDevice().executeShellCommand(cleanupCmd, bitBucket, MAX_OP_TIME);

                    String output = receiver.getOutput();
                    Log.v(LOG_TAG, String.format("%s returned %s", checkCmd, output));
                    if (output.contains(testString)) {
                        return true;
                    }
                } catch (IOException e) {
                    Log.i(LOG_TAG, String.format("%s failed: %s", cmd, e.getMessage()));
                } catch (TimeoutException e) {
                    Log.i(LOG_TAG, String.format("%s failed: timeout", cmd));
                } catch (AdbCommandRejectedException e) {
                    Log.i(LOG_TAG, String.format("%s failed: %s", cmd, e.getMessage()));
                } catch (ShellCommandUnresponsiveException e) {
                    Log.i(LOG_TAG, String.format("%s failed: %s", cmd, e.getMessage()));
                }
            } else {
                Log.w(LOG_TAG, String.format("Failed to get external store mount point for %s",
                        getSerialNumber()));
            }
            getRunUtil().sleep(CHECK_POLL_TIME);
        }
        Log.w(LOG_TAG, String.format("Device %s external storage is not mounted after %d ms",
                getSerialNumber(), waitTime));
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPoint(String mountName) {
        String mountPoint = getIDevice().getMountPoint(mountName);
        if (mountPoint != null) {
            return mountPoint;
        }
        // cached mount point is null - try querying directly
        CollectingOutputReceiver receiver = new CollectingOutputReceiver();
        try {
            getIDevice().executeShellCommand("echo $" + mountName, receiver);
            return receiver.getOutput().trim();
        } catch (IOException e) {
            return null;
        } catch (TimeoutException e) {
            return null;
        } catch (AdbCommandRejectedException e) {
            return null;
        } catch (ShellCommandUnresponsiveException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TestDeviceState getDeviceState() {
        return mDeviceState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean waitForDeviceBootloader(long time) {
        if (!mFastbootEnabled) {
            return false;
        }
        long startTime = System.currentTimeMillis();
        // ensure fastboot state is updated at least once
        waitForDeviceBootloaderStateUpdate();
        long elapsedTime = System.currentTimeMillis() - startTime;
        IFastbootListener listener = new StubFastbootListener();
        mMgr.addFastbootListener(listener);
        long waitTime = time - elapsedTime;
        if (waitTime < 0) {
            // wait at least 200ms
            waitTime = 200;
        }
        boolean result =  waitForDeviceState(TestDeviceState.FASTBOOT, waitTime);
        mMgr.removeFastbootListener(listener);
        return result;
    }

    @Override
    public void waitForDeviceBootloaderStateUpdate() {
        if (!mFastbootEnabled) {
            return;
        }
        IFastbootListener listener = new NotifyFastbootListener();
        synchronized (listener) {
            mMgr.addFastbootListener(listener);
            try {
                listener.wait();
            } catch (InterruptedException e) {
                Log.w(LOG_TAG, "wait for device bootloader state update interrupted");
            }
        }
        mMgr.removeFastbootListener(listener);
    }

    private boolean waitForDeviceState(TestDeviceState state, long time) {
        String deviceSerial = getSerialNumber();
        if (getDeviceState() == state) {
            Log.i(LOG_TAG, String.format("Device %s is already %s", deviceSerial, state));
            return true;
        }
        Log.i(LOG_TAG, String.format("Waiting for device %s to be %s; it is currently %s...",
                deviceSerial, state, getDeviceState()));
        DeviceStateListener listener = new DeviceStateListener(state);
        addDeviceStateListener(listener);
        synchronized (listener) {
            try {
                listener.wait(time);
            } catch (InterruptedException e) {
                Log.w(LOG_TAG, "wait for device state interrupted");
            }
        }
        removeDeviceStateListener(listener);
        return getDeviceState().equals(state);
    }

    /**
     * @param listener
     */
    private void removeDeviceStateListener(DeviceStateListener listener) {
        synchronized (mStateListeners) {
            mStateListeners.remove(listener);
        }
    }

    /**
     * @param listener
     */
    private void addDeviceStateListener(DeviceStateListener listener) {
        synchronized (mStateListeners) {
            mStateListeners.add(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(TestDeviceState deviceState) {
        mDeviceState = deviceState;
        // create a copy of listeners to prevent holding mStateListeners lock when notifying
        // and to protect from list modification when iterating
        Collection<DeviceStateListener> listenerCopy = new ArrayList<DeviceStateListener>(
                mStateListeners.size());
        synchronized (mStateListeners) {
            listenerCopy.addAll(mStateListeners);
        }
        for (DeviceStateListener listener: listenerCopy) {
            listener.stateChanged(deviceState);
        }
    }

    @Override
    public void setIDevice(IDevice newDevice) {
        IDevice currentDevice = mDevice;
        if (!getIDevice().equals(newDevice)) {
            synchronized (currentDevice) {
                mDevice = newDevice;
            }
        }
    }

    private static class DeviceStateListener {
        private final TestDeviceState mExpectedState;

        public DeviceStateListener(TestDeviceState expectedState) {
            mExpectedState = expectedState;
        }

        public void stateChanged(TestDeviceState newState) {
            if (mExpectedState.equals(newState)) {
                synchronized (this) {
                    notify();
                }
            }
        }
    }

    /**
     * An empty implementation of {@link IFastbootListener}
     */
    private static class StubFastbootListener implements IFastbootListener {
        @Override
        public void stateUpdated() {
            // ignore
        }
    }

    /**
     * A {@link IFastbootListener} that notifies when a status update has been received.
     */
    private static class NotifyFastbootListener implements IFastbootListener {
        @Override
        public void stateUpdated() {
            synchronized (this) {
                notify();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAdbTcp() {
        return mDevice.getSerialNumber().contains(":");
    }
}