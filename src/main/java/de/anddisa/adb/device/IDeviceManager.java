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

import com.android.ddmlib.AndroidDebugBridge;
import de.anddisa.adb.util.IRunUtil;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;

/**
 * Interface for managing the set of available devices for testing.
 */
public interface IDeviceManager {

    public enum FreeDeviceState {
        /** device is responsive, and can be returned to the available device queue */
        AVAILABLE,

        /**
         * Device is not visible via adb, and should not be returned to the available device
         * queue
         */
        UNAVAILABLE,

        /**
         * Device is visible on adb, but is not responsive. Depending on configuration this
         * device may be returned to available queue.
         */
        UNRESPONSIVE,

        /**
         * Device should be ignored, and not returned to the available device queue.
         */
        IGNORE;
    }

    /**
     * A listener for fastboot state changes.
     */
    public static interface IFastbootListener {
        /**
         * Callback when fastboot state has been updated for all devices.
         */
        public void stateUpdated();
    }

    /**
     * Initialize the device manager. This must be called once and only once before any other
     * methods are called.
     */
    public void init(String pathToAdb);

    /**
     * Initialize the device manager with a device filter. This filter can be used to instruct
     * the DeviceManager to ignore certain connected devices.
     */
    public void init(IDeviceSelection globalDeviceFilter, String pathToAdb);

    /**
     * Request a physical device for testing, waiting indefinitely until one becomes available.
     *
     * @return a {@link ITestDevice} for testing, or <code>null</code> if interrupted
     */
    public ITestDevice allocateDevice();

    /**
     * Request a physical device for testing, waiting for timeout ms until one becomes available.
     *
     * @param timeout max time in ms to wait for a device to become available.
     * @return a {@link ITestDevice} for testing, or <code>null</code> if timeout expired before one
     *         became available
     */
    public ITestDevice allocateDevice(long timeout);

    /**
     * Request a device for testing that meets certain criteria.
     *
     * @param timeout max time in ms to wait for a device to become available.
     * @param options the {@link IDeviceSelection} the device should meet.
     * @return a {@link ITestDevice} for testing, or <code>null</code> if timeout expired before one
     *         became available
     */
    public ITestDevice allocateDevice(long timeout, IDeviceSelection options);

    /**
     * Rudely allocate a device, even if its not currently available.
     * <p/>
     * Will have no effect if device is already allocated.
     *
     * @param serial the device serial to allocate
     * @return the {@link ITestDevice}, or <code>null</code> if it could not be allocated
     */
    public ITestDevice forceAllocateDevice(String serial);

    /**
     * Return a device to the pool
     * <p/>
     * Attempts to return a device that hasn't been previously allocated will be ignored.
     *
     * @param device the {@link ITestDevice} to free
     * @param state the {@link FreeDeviceState}. Used to control if device is returned to available
     *            device pool.
     */
    public void freeDevice(ITestDevice device, FreeDeviceState state);

    /**
     * Helper method to launch emulator.
     * <p/>
     * Will launch the emulator on the port specified in the allocated {@link ITestDevice}.
     * Blocks until emulator launches successfully.
     *
     * @param device the placeholder {@link ITestDevice} representing allocated emulator device
     * @throws DeviceNotAvailableException if emulator fails to boot or come online
     */
    void launchEmulator(ITestDevice device, long bootTimeout, IRunUtil runUtil,
            List<String> emulatorArgs) throws DeviceNotAvailableException;

    /**
     * Shut down the given emulator.
     * <p/>
     * Blocks until emulator disappears from adb. Will have no effect if emulator is already not
     * available.
     *
     * @param device the {@link ITestDevice} representing emulator to shut down
     * @throws DeviceNotAvailableException if emulator fails to shut down
     */
    public void killEmulator(ITestDevice device) throws DeviceNotAvailableException;

    /**
     * Connect to a device with adb-over-tcp
     * <p/>
     * This method allocates a new device, which should eventually be freed via
     * {@link #disconnectFromTcpDevice(ITestDevice))}
     * <p/>
     * The returned {@link ITestDevice} will be online, but may not be responsive.
     * <p/>
     * Note that performing action such as a reboot on a tcp connected device, will sever the
     * tcp connection to the device, and result in a {@link DeviceNotAvailableException}
     *
     * @param ipAndPort the original ip address and port of the device to connect to
     * @return the {@link ITestDevice} or <code>null</code> if a tcp connection could not be formed
     */
    public ITestDevice connectToTcpDevice(String ipAndPort);

    /**
     * Disconnect from an adb-over-tcp connected device.
     * <p/>
     * Switches the device back to usb mode, and frees it.
     *
     * @param tcpDevice the device currently in tcp mode, previously allocated via
     *            {@link connectToTcpDevice}
     * @return <code>true</code> if switch to usb mode was successful
     */
    public boolean disconnectFromTcpDevice(ITestDevice tcpDevice);

    /**
     * A helper method that switches the given usb device to adb-over-tcp mode, and then connects to
     * it via {@link #connectToTcpDevice(String)}.
     *
     * @param usbDevice the device currently in usb mode
     * @return the newly allocated {@link ITestDevice} in tcp mode or <code>null</code> if a tcp
     *         connection could not be formed
     * @throws DeviceNotAvailableException if the connection with <var>usbDevice</var> was lost and
     *             could not be recovered
     */
    public ITestDevice reconnectDeviceToTcp(ITestDevice usbDevice)
            throws DeviceNotAvailableException;

    /**
     * Stops device monitoring services, and terminates the ddm library.
     * <p/>
     * This must be called upon application termination.
     *
     * @see AndroidDebugBridge#terminate()
     */
    public void terminate();

    /**
     * Like {@link #terminate()}, but attempts to forcefully shut down adb as well.
     */
    public void terminateHard();

    /**
     * Diagnostic method that returns a list of the devices available for allocation.
     *
     * @return a {@link Collection} of device serials
     */
    public Collection<String> getAvailableDevices();

    /**
     * Diagnostic method that returns a list of the devices currently allocated for testing.
     *
     * @return a {@link Collection} of device serials
     */
    public Collection<String> getAllocatedDevices();

    /**
     * Diagnostic method that returns a list of the devices currently visible via adb, but not
     * deemed available for allocation.
     *
     * @return a {@link Collection} of device serials
     */
    public Collection<String> getUnavailableDevices();

    /**
     * Output a user-friendly description containing list of known devices, their state, and
     * values for commonly used {@link IDeviceSelection} options.
     *
     * @param printWriter the {@link PrintWriter} to output the description to
     */
    public void displayDevicesInfo(PrintWriter printWriter);

    /**
     * Informs the manager that a listener is interested in fastboot state changes.
     * <p/>
     * Currently a {@link IDeviceManager} will only monitor devices in fastboot if there are one or
     * more active listeners.
     * <p/>
     * TODO: this is a bit of a hack - find a better solution
     *
     * @param listener
     */
    public void addFastbootListener(IFastbootListener listener);

    /**
     * Informs the manager that a listener is no longer interested in fastboot state changes.
     * @param listener
     */
    public void removeFastbootListener(IFastbootListener listener);

}
