/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache
 * License, Version 2.0 (the "License");
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
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import de.anddisa.adb.log.LogUtil.CLog;
import de.anddisa.adb.util.IRunUtil;
import de.anddisa.adb.util.RunUtil;

import java.io.IOException;

/**
 * Runs a command on a given device repeating as necessary until the action is canceled.
 * <p>
 * When the class is run, the command is run on the device in a separate thread and the output is
 * collected in a temporary host file.
 * </p><p>
 * This is done so:
 * </p><ul>
 * <li>if device goes permanently offline during a test, the log data is retained.</li>
 * <li>to capture more data than may fit in device's circular log.</li>
 * </ul>
 */
public class BackgroundDeviceAction extends Thread {
    private IShellOutputReceiver mReceiver;
    private ITestDevice mTestDevice;
    private String mCommand;
    private String mSerialNumber;
    private String mDescriptor;
    private boolean mIsCancelled;
    private int mLogStartDelay;

    /**
     * Creates a {@link BackgroundDeviceAction}
     *
     * @param command the command to run
     * @param descriptor the description of the command. For logging only.
     * @param device the device to run the command on
     * @param receiver the receiver for collecting the output of the command
     * @param startDelay the delay to wait after the device becomes online
     */
    public BackgroundDeviceAction(String command, String descriptor, ITestDevice device,
            IShellOutputReceiver receiver, int startDelay) {
        mCommand = command;
        mDescriptor = descriptor;
        mSerialNumber = device.getSerialNumber();
        mTestDevice = device;
        mReceiver = receiver;
        mLogStartDelay = startDelay;
        // don't keep VM open if this thread is still running
        setDaemon(true);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Repeats the command until canceled.
     * </p>
     */
    @Override
    public void run() {
        while (!isCancelled()) {
            if (mLogStartDelay > 0) {
                CLog.d("Sleep for %d before starting %s for %s.", mLogStartDelay, mDescriptor,
                        mSerialNumber);
                getRunUtil().sleep(mLogStartDelay);
            }
            CLog.d("Starting %s for %s.", mDescriptor, mSerialNumber);
            try {
                mTestDevice.getIDevice().executeShellCommand(mCommand, mReceiver, 0);
            } catch (TimeoutException e) {
                recoverDevice(e.getClass().getName());
            } catch (AdbCommandRejectedException e) {
                recoverDevice(e.getClass().getName());
            } catch (ShellCommandUnresponsiveException e) {
                recoverDevice(e.getClass().getName());
            } catch (IOException e) {
                recoverDevice(e.getClass().getName());
            }
        }
    }

    private void recoverDevice(String exceptionType) {
        CLog.d("%s while running %s on %s. May see duplicated content in log.", exceptionType,
                mDescriptor, mSerialNumber);

        // FIXME: Determine when we should append a message to the receiver.
        if (mReceiver instanceof LargeOutputReceiver) {
            ((LargeOutputReceiver) mReceiver).appendLogMsg(String.format(
                    "%s interrupted. May see duplicated content in log.", mDescriptor));
        }

        // Make sure we haven't been cancelled before we sleep for a long time
        if (isCancelled()) {
            return;
        }

        // sleep a small amount for device to settle
        getRunUtil().sleep(5 * 1000);

        // wait a long time for device to be online
        try {
            mTestDevice.waitForDeviceOnline(10 * 60 * 1000);
        } catch (DeviceNotAvailableException e) {
            CLog.w("Device %s not online", mSerialNumber);
        }
    }

    /**
     * Cancels the command.
     */
    public synchronized void cancel() {
        mIsCancelled = true;
        interrupt();
    }

    /**
     * If the command is cancelled.
     */
    public synchronized boolean isCancelled() {
        return mIsCancelled;
    }

    /**
     * Get the {@link RunUtil} instance to use.
     * <p/>
     * Exposed for unit testing.
     */
    IRunUtil getRunUtil() {
        return RunUtil.getDefault();
    }
}
