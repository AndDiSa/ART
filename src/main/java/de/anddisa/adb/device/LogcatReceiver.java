/*
 * Copyright (C) 2012 The Android Open Source Project
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

import de.anddisa.adb.result.InputStreamSource;

/**
 * Class that collects logcat in background. Continues to capture logcat even if device goes
 * offline then online.
 */
public class LogcatReceiver {
    private BackgroundDeviceAction mDeviceAction;
    private LargeOutputReceiver mReceiver;

    static final String LOGCAT_CMD = "logcat -v threadtime";
    private static final String LOGCAT_DESC = "logcat";

    public LogcatReceiver(ITestDevice device, long maxFileSize, int logStartDelay) {
        mReceiver = new LargeOutputReceiver(LOGCAT_DESC, device.getSerialNumber(),
                maxFileSize);
        // FIXME: remove mLogStartDelay. Currently delay starting logcat, as starting
        // immediately after a device comes online has caused adb instability
        mDeviceAction = new BackgroundDeviceAction(LOGCAT_CMD, LOGCAT_DESC, device,
                mReceiver, logStartDelay);
    }

    public void start() {
        mDeviceAction.start();
    }

    public void stop() {
        mDeviceAction.cancel();
        mReceiver.cancel();
        mReceiver.delete();
    }

    public InputStreamSource getLogcatData() {
        return mReceiver.getData();
    }

    public void clear() {
        mReceiver.clear();
    }
}
