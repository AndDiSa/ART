/*
 * Copyright (C) 2011 The Android Open Source Project
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

import de.anddisa.adb.config.Option;

/**
 * Container for {@link ITestDevice} {@link Option}s
 */
public class TestDeviceOptions {

    @Option(name = "enable-root", description = "enable adb root on boot.")
    private boolean mEnableAdbRoot = true;

    @Option(name = "disable-keyguard", description = "attempt to disable keyguard once complete.")
    private boolean mDisableKeyguard = true;

    @Option(name = "disable-keyguard-cmd", description = "shell command to disable keyguard.")
    private String mDisableKeyguardCmd = "input keyevent 82";

    @Option(name = "max-tmp-logcat-file", description =
        "The maximum size of a tmp logcat file, in bytes.")
    private long mMaxLogcatFileSize = 10 * 1024 * 1024;

    @Option(name = "fastboot-timeout", description =
            "time in ms to wait for a device to boot into fastboot.")
    private int mFastbootTimeout = 1 * 60 * 1000;

    @Option(name = "adb-recovery-timeout", description =
            "time in ms to wait for a device to boot into recovery.")
    private int mAdbRecoveryTimeout = 1 * 60 * 1000;

    @Option(name = "reboot-timeout", description =
            "time in ms to wait for a device to reboot to full system.")
    private int mRebootTimeout = 2 * 60 * 1000;

    @Option(name = "use-fastboot-erase", description =
            "use fastboot erase instead of fastboot format to wipe partitions")
    private boolean mUseFastbootErase = false;

    @Option(name = "unencrypt-reboot-timeout", description = "time in ms to wait for the device to "
            + "format the filesystem and reboot after unencryption")
    private int mUnencryptRebootTimeout = 0;

    @Option(name = "online-timeout", description = "default time in ms to wait for the device to "
            + "be visible on adb.")
    private long mOnlineTimeout = 1 * 60 * 1000;

    @Option(name = "available-timeout", description = "default time in ms to wait for the device "
            + "to be available aka fully boot.")
    private long mAvailableTimeout = 6 * 60 * 1000;

    /**
     * @return the mEnableAdbRoot
     */
    public boolean isEnableAdbRoot() {
        return mEnableAdbRoot;
    }

    /**
     * @param mEnableAdbRoot the mEnableAdbRoot to set
     */
    public void setEnableAdbRoot(boolean enableAdbRoot) {
        mEnableAdbRoot = enableAdbRoot;
    }

    /**
     * @return the mDisableKeyguard
     */
    public boolean isDisableKeyguard() {
        return mDisableKeyguard;
    }

    /**
     * @param mDisableKeyguard the mDisableKeyguard to set
     */
    public void setDisableKeyguard(boolean disableKeyguard) {
        mDisableKeyguard = disableKeyguard;
    }

    /**
     * @return the mDisableKeyguardCmd
     */
    public String getDisableKeyguardCmd() {
        return mDisableKeyguardCmd;
    }

    /**
     * @param mDisableKeyguardCmd the mDisableKeyguardCmd to set
     */
    public void setDisableKeyguardCmd(String disableKeyguardCmd) {
        mDisableKeyguardCmd = disableKeyguardCmd;
    }

    /**
     * Get the maximum size of a tmp logcat file, in bytes.
     * <p/>
     * The actual size of the log info stored will be up to twice this number, as two logcat files
     * are stored.
     *
     * TODO: make this represent a strictly enforced total max size
     */
    public long getMaxLogcatFileSize() {
        return mMaxLogcatFileSize;
    }

    /**
     * @param maxLogcatFileSize the max logcat file size to set
     */
    public void setMaxLogcatFileSize(long maxLogcatFileSize) {
        mMaxLogcatFileSize = maxLogcatFileSize;
    }

    /**
     * @return the timeout to boot into fastboot mode in msecs.
     */
    public int getFastbootTimeout() {
        return mFastbootTimeout;
    }

    /**
     * @param fastbootTimeout the timout in msecs to boot into fastboot mode.
     */
    public void setFastbootTimeout(int fastbootTimeout) {
        mFastbootTimeout = fastbootTimeout;
    }

    /**
     * @return the timeout in msecs to boot into recovery mode.
     */
    public int getAdbRecoveryTimeout() {
        return mAdbRecoveryTimeout;
    }

    /**
     * @param adbRecoveryTimeout the timeout in msecs to boot into recovery mode.
     */
    public void setAdbRecoveryTimeout(int adbRecoveryTimeout) {
        mAdbRecoveryTimeout = adbRecoveryTimeout;
    }

    /**
     * @return the timeout in msecs for the full system boot.
     */
    public int getRebootTimeout() {
        return mRebootTimeout;
    }

    /**
     * @param mRebootTimeout the timeout in msecs for the system to fully boot.
     */
    public void setRebootTimeout(int mRebootTimeout) {
        this.mRebootTimeout = mRebootTimeout;
    }

    /**
     * @return whether to use fastboot erase instead of fastboot format to wipe partitions.
     */
    public boolean getUseFastbootErase() {
        return mUseFastbootErase;
    }

    /**
     * @param useFastbootErase whether to use fastboot erase instead of fastboot format to wipe
     * partitions.
     */
    public void setUseFastbootErase(boolean useFastbootErase) {
        mUseFastbootErase = useFastbootErase;
    }

    /**
     * @return the timeout in msecs for the filesystem to be formatted and the device to reboot
     * after unencryption.
     */
    public int getUnencryptRebootTimeout() {
        return mUnencryptRebootTimeout;
    }

    /**
     * @param unencryptRebootTimeout the timeout in msecs for the filesystem to be formatted and
     * the device to reboot after unencryption.
     */
    public void setUnencryptRebootTimeout(int unencryptRebootTimeout) {
        mUnencryptRebootTimeout = unencryptRebootTimeout;
    }

    /**
     * @return the default time in ms to to wait for a device to be online.
     */
    public long getOnlineTimeout() {
        return mOnlineTimeout;
    }

    public void setOnlineTimeout(long onlineTimeout) {
        mOnlineTimeout = onlineTimeout;
    }

    /**
     * @return the default time in ms to to wait for a device to be available.
     */
    public long getAvailableTimeout() {
        return mAvailableTimeout;
    }
}