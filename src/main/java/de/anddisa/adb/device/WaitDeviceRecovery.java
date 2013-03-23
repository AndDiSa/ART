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

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.TimeoutException;
import de.anddisa.adb.config.Option;
import de.anddisa.adb.log.LogUtil.CLog;
import de.anddisa.adb.util.IRunUtil;
import de.anddisa.adb.util.RunUtil;

import java.io.IOException;

/**
 * A simple implementation of a {@link IDeviceRecovery} that waits for device to be online and
 * respond to simple commands.
 */
public class WaitDeviceRecovery implements IDeviceRecovery {

    private static final String LOG_TAG = "WaitDeviceRecovery";

    /** the time in ms to wait before beginning recovery attempts */
    protected static final long INITIAL_PAUSE_TIME = 5 * 1000;

    /**
     * The number of attempts to check if device is in bootloader.
     * <p/>
     * Exposed for unit testing
     */
    public static final int BOOTLOADER_POLL_ATTEMPTS = 3;

    // TODO: add a separate configurable timeout per operation
    @Option(name="device-wait-time",
            description="maximum time in ms to wait for a single device recovery command.")
    protected long mWaitTime = 4 * 60 * 1000;

    @Option(name="bootloader-wait-time",
            description="maximum time in ms to wait for device to be in fastboot.")
    protected long mBootloaderWaitTime = 30 * 1000;

    @Option(name="shell-wait-time",
            description="maximum time in ms to wait for device shell to be responsive.")
    protected long mShellWaitTime = 30 * 1000;

    /**
     * Get the {@link RunUtil} instance to use.
     * <p/>
     * Exposed for unit testing.
     */
    protected IRunUtil getRunUtil() {
        return RunUtil.getDefault();
    }

    /**
     * Sets the maximum time in ms to wait for a single device recovery command.
     */
    void setWaitTime(long waitTime) {
        mWaitTime = waitTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recoverDevice(IDeviceStateMonitor monitor, boolean recoverUntilOnline)
            throws DeviceNotAvailableException {
        // device may have just gone offline
        // sleep a small amount to give ddms state a chance to settle
        // TODO - see if there is better way to handle this
        Log.i(LOG_TAG, String.format("Pausing for %d for %s to recover",
                INITIAL_PAUSE_TIME, monitor.getSerialNumber()));
        getRunUtil().sleep(INITIAL_PAUSE_TIME);

        // ensure bootloader state is updated
        monitor.waitForDeviceBootloaderStateUpdate();

        if (monitor.getDeviceState().equals(TestDeviceState.FASTBOOT)) {
            Log.i(LOG_TAG, String.format(
                    "Found device %s in fastboot but expected online. Rebooting...",
                    monitor.getSerialNumber()));
            // TODO: retry if failed
            getRunUtil().runTimedCmd(20*1000, "fastboot", "-s", monitor.getSerialNumber(),
                    "reboot");
        }

        // wait for device online
        IDevice device = monitor.waitForDeviceOnline();
        if (device == null) {
            handleDeviceNotAvailable(monitor, recoverUntilOnline);
            return;
        }
        // occasionally device is erroneously reported as online - double check that we can shell
        // into device
        if (!monitor.waitForDeviceShell(mShellWaitTime)) {
            // treat this as a not available device
            handleDeviceNotAvailable(monitor, recoverUntilOnline);
            return;
        }

        if (!recoverUntilOnline) {
            if (monitor.waitForDeviceAvailable(mWaitTime) == null) {
                // device is online but not responsive
                handleDeviceUnresponsive(device, monitor);
            }
        }
    }

    /**
     * Handle situation where device is online but unresponsive.
     * @param monitor
     * @throws DeviceNotAvailableException
     */
    protected void handleDeviceUnresponsive(IDevice device, IDeviceStateMonitor monitor)
            throws DeviceNotAvailableException {
        rebootDevice(device);
        IDevice newdevice = monitor.waitForDeviceOnline();
        if (newdevice == null) {
            handleDeviceNotAvailable(monitor, false);
            return;
        }
        if (monitor.waitForDeviceAvailable(mWaitTime) == null) {
            throw new DeviceUnresponsiveException(String.format(
                    "Device %s is online but unresponsive", monitor.getSerialNumber()));
        }
    }

    /**
     * Handle situation where device is not available.
     *
     * @param monitor the {@link IDeviceStateMonitor}
     * @param recoverTillOnline if true this method should return if device is online, and not
     * check for responsiveness
     * @throws DeviceNotAvailableException
     */
    protected void handleDeviceNotAvailable(IDeviceStateMonitor monitor, boolean recoverTillOnline)
            throws DeviceNotAvailableException {
        throw new DeviceNotAvailableException(String.format("Could not find device %s",
                monitor.getSerialNumber()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recoverDeviceBootloader(final IDeviceStateMonitor monitor)
            throws DeviceNotAvailableException {
        // device may have just gone offline
        // wait a small amount to give device state a chance to settle
        // TODO - see if there is better way to handle this
        Log.i(LOG_TAG, String.format("Pausing for %d for %s to recover",
                INITIAL_PAUSE_TIME, monitor.getSerialNumber()));
        getRunUtil().sleep(INITIAL_PAUSE_TIME);

        // poll and wait for device to return to valid state
        long pollTime = mBootloaderWaitTime / BOOTLOADER_POLL_ATTEMPTS;
        for (int i=0; i < BOOTLOADER_POLL_ATTEMPTS; i++) {
            if (monitor.waitForDeviceBootloader(pollTime)) {
                handleDeviceBootloaderUnresponsive(monitor);
                // passed above check, abort
                return;
            } else if (monitor.getDeviceState() == TestDeviceState.ONLINE) {
                handleDeviceOnlineExpectedBootloader(monitor);
                return;
            }
        }
        handleDeviceBootloaderNotAvailable(monitor);
    }

    /**
     * Handle condition where device is online, but should be in bootloader state.
     * <p/>
     * If this method
     * @param monitor
     * @throws DeviceNotAvailableException
     */
    protected void handleDeviceOnlineExpectedBootloader(final IDeviceStateMonitor monitor)
            throws DeviceNotAvailableException {
        Log.i(LOG_TAG, String.format("Found device %s online but expected fastboot.",
            monitor.getSerialNumber()));
        // call waitForDeviceOnline to get handle to IDevice
        IDevice device = monitor.waitForDeviceOnline();
        if (device == null) {
            handleDeviceBootloaderNotAvailable(monitor);
            return;
        }
        rebootDeviceIntoBootloader(device);
        if (!monitor.waitForDeviceBootloader(mBootloaderWaitTime)) {
            throw new DeviceNotAvailableException(String.format(
                    "Device %s not in bootloader after reboot", monitor.getSerialNumber()));
        }
    }

    /**
     * @param monitor
     * @throws DeviceNotAvailableException
     */
    protected void handleDeviceBootloaderUnresponsive(IDeviceStateMonitor monitor)
            throws DeviceNotAvailableException {
        CLog.i("Found device %s in fastboot but potentially unresponsive.",
                monitor.getSerialNumber());
        // TODO: retry reboot
        getRunUtil().runTimedCmd(20*1000, "fastboot", "-s", monitor.getSerialNumber(),
                "reboot-bootloader");
        // wait for device to reboot
        monitor.waitForDeviceNotAvailable(20*1000);
        if (!monitor.waitForDeviceBootloader(mBootloaderWaitTime)) {
            throw new DeviceNotAvailableException(String.format(
                    "Device %s not in bootloader after reboot", monitor.getSerialNumber()));
        }
    }

    /**
     * Reboot device into bootloader.
     *
     * @param device the {@link IDevice} to reboot.
     */
    protected void rebootDeviceIntoBootloader(IDevice device) {
        try {
            device.reboot("bootloader");
        } catch (IOException e) {
            Log.w(LOG_TAG, String.format("failed to reboot %s: %s", device.getSerialNumber(),
                    e.getMessage()));
        } catch (TimeoutException e) {
            Log.w(LOG_TAG, String.format("failed to reboot %s: timeout", device.getSerialNumber()));
        } catch (AdbCommandRejectedException e) {
            Log.w(LOG_TAG, String.format("failed to reboot %s: %s", device.getSerialNumber(),
                    e.getMessage()));
        }
    }

    /**
     * Reboot device into bootloader.
     *
     * @param device the {@link IDevice} to reboot.
     */
    protected void rebootDevice(IDevice device) {
        try {
            device.reboot(null);
        } catch (IOException e) {
            Log.w(LOG_TAG, String.format("failed to reboot %s: %s", device.getSerialNumber(),
                    e.getMessage()));
        } catch (TimeoutException e) {
            Log.w(LOG_TAG, String.format("failed to reboot %s: timeout", device.getSerialNumber()));
        } catch (AdbCommandRejectedException e) {
            Log.w(LOG_TAG, String.format("failed to reboot %s: %s", device.getSerialNumber(),
                    e.getMessage()));
        }
    }

    /**
     * Handle situation where device is not available when expected to be in bootloader.
     *
     * @param monitor the {@link IDeviceStateMonitor}
     * @throws DeviceNotAvailableException
     */
    protected void handleDeviceBootloaderNotAvailable(final IDeviceStateMonitor monitor)
            throws DeviceNotAvailableException {
        throw new DeviceNotAvailableException(String.format(
                "Could not find device %s in bootloader", monitor.getSerialNumber()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recoverDeviceRecovery(IDeviceStateMonitor monitor)
            throws DeviceNotAvailableException {
        throw new DeviceNotAvailableException("device recovery not implemented");
    }
}
