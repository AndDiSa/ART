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

import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.ITestRunListener;
import de.anddisa.adb.build.IBuildInfo;
import de.anddisa.adb.result.InputStreamSource;
import de.anddisa.adb.util.CommandResult;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides an reliable and slightly higher level API to a ddmlib {@link IDevice}.
 * <p/>
 * Retries device commands for a configurable amount, and provides a device recovery
 * interface for devices which are unresponsive.
 */
public interface ITestDevice {

    public enum RecoveryMode {
        /** don't attempt to recover device. */
        NONE,
        /** recover device to online state only */
        ONLINE,
        /**
         * Recover device into fully testable state - framework is up, and external storage is
         * mounted.
         */
        AVAILABLE
    }

    /**
     * A simple struct class to store information about a single mountpoint
     */
    public static class MountPointInfo {
        public String filesystem;
        public String mountpoint;
        public String type;
        public List<String> options;

        /** Simple constructor */
        public MountPointInfo() {}

        /**
         * Convenience constructor to set all members
         */
        public MountPointInfo(String filesystem, String mountpoint, String type,
                List<String> options) {
            this.filesystem = filesystem;
            this.mountpoint = mountpoint;
            this.type = type;
            this.options = options;
        }

        public MountPointInfo(String filesystem, String mountpoint, String type, String optString) {
            this(filesystem, mountpoint, type, splitMountOptions(optString));
        }

        public static List<String> splitMountOptions(String options) {
            List<String> list = Arrays.asList(options.split(","));
            return list;
        }

        @Override
        public String toString() {
            return String.format("%s %s %s %s", this.filesystem, this.mountpoint, this.type, this.options);
        }
    }

    public PartitionInfo getSystemPartition();
    public PartitionInfo getDataPartition();
    public PartitionInfo getCachePartition();
    public PartitionInfo getMiscPartition();
    public PartitionInfo getBootPartition();
    public PartitionInfo getRecoveryPartition();
    public PartitionInfo getPartition(String partitionName);
    
    /**
     * Set the {@link IDeviceRecovery} to use for this device. Should be set when device is first
     * allocated.
     *
     * @param recovery the {@link IDeviceRecovery}
     */
    public void setRecovery(IDeviceRecovery recovery);

    /**
     * Set the current recovery mode to use for the device.
     * <p/>
     * Used to control what recovery method to use when a device communication problem is
     * encountered. Its recommended to only use this method sparingly when needed (for example,
     * when framework is down, etc
     *
     * @param mode whether 'recover till online only' mode should be on or not.
     */
    public void setRecoveryMode(RecoveryMode mode);

    /**
     * Get the current recovery mode used for the device.
     *
     * @return the current recovery mode used for the device.
     */
    public RecoveryMode getRecoveryMode();

    /**
     * Returns a reference to the associated ddmlib {@link IDevice}.
     * <p/>
     * A new {@link IDevice} may be allocated by DDMS each time the device disconnects and
     * reconnects from adb. Thus callers should not keep a reference to the {@link IDevice},
     * because that reference may become stale.
     *
     * @return the {@link IDevice}
     */
    public IDevice getIDevice();

    /**
     * Convenience method to get serial number of this device.
     *
     * @return the {@link String} serial number
     */
    public String getSerialNumber();

    /**
     * Convenience method to get the product type of this device.
     * <p/>
     * This method will work if device is in either adb or fastboot mode.
     *
     * @return the {@link String} product type name or <code>null</code> if it cannot be determined
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public String getProductType() throws DeviceNotAvailableException;

    /**
     * Convenience method to get the product variant of this device.
     * <p/>
     * This method will work if device is in either adb or fastboot mode.
     *
     * @return the {@link String} product variant name or <code>null</code> if it cannot be
     *         determined
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public String getProductVariant() throws DeviceNotAvailableException;

    /**
     * Convenience method to get the product type of this device when its in fastboot mode.
     * <p/>
     * This method should only be used if device should be in fastboot. Its a bit safer variant
     * than the generic {@link #getProductType()} method in this case, because ITestDevice
     * will know to recover device into fastboot if device is in incorrect state or is
     * unresponsive.
     *
     * @return the {@link String} product type name or <code>null</code> if it cannot be determined
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public String getFastbootProductType() throws DeviceNotAvailableException;

    /**
     * Convenience method to get the product type of this device when its in fastboot mode.
     * <p/>
     * This method should only be used if device should be in fastboot. Its a bit safer variant
     * than the generic {@link #getProductType()} method in this case, because ITestDevice
     * will know to recover device into fastboot if device is in incorrect state or is
     * unresponsive.
     *
     * @return the {@link String} product type name or <code>null</code> if it cannot be determined
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public String getFastbootProductVariant() throws DeviceNotAvailableException;

    /**
     * Convenience method to get the bootloader version of this device.
     * <p/>
     * Will attempt to retrieve bootloader version from the device's current state. (ie if device
     * is in fastboot mode, it will attempt to retrieve version from fastboot)
     *
     * @return the {@link String} bootloader version or <code>null</code> if it cannot be dounf
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public String getBootloaderVersion() throws DeviceNotAvailableException;

    /**
     * Retrieve the build the device is currently running.
     *
     * @return the build id or {@link IBuildInfo#UNKNOWN_BUILD_ID} if it could not be retrieved
     */
    public String getBuildId();

    /**
     * Retrieve the given cached property value from the device.
     * <p/>
     * Note this method should only be used for read-only properties that won't change after
     * device comes online. For volatile properties, use {@link #getPropertySync()}
     *
     * @param name the property name
     * @return the property value or <code>null</code> if it does not exist
     * @throws DeviceNotAvailableException
     */
    public String getProperty(String name) throws DeviceNotAvailableException;

    /**
     * Retrieve the given property value from the device.
     * <p/>
     * Note this method performs a live query against device. Its recommended to use
     * {@link #getProperty(String)} instead for read-only properties.
     *
     * @param name the property name
     * @return the property value or <code>null</code> if it does not exist
     * @throws DeviceNotAvailableException
     */
    public String getPropertySync(String name) throws DeviceNotAvailableException;

    /**
     * Executes the given adb shell command, retrying multiple times if command fails.
     * <p/>
     * A simpler form of {@link #executeShellCommand(String, IShellOutputReceiver, int, int))} with
     * default values.
     *
     * @param command the adb shell command to run
     * @param receiver the {@link IShellOutputReceiver} to direct shell output to.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public void executeShellCommand(String command, IShellOutputReceiver receiver)
        throws DeviceNotAvailableException;

    /**
     * Executes a adb shell command, with more parameters to control command behavior.
     *
     * @see {@link IDevice#executeShellCommand(String, IShellOutputReceiver, int)}
     * @param command the adb shell command to run
     * @param receiver the {@link IShellOutputReceiver} to direct shell output to.
     * @param maxTimeToOutputShellResponse the maximum amount of time during which the command is
     *            allowed to not output any response.
     * @param retryAttempts the maximum number of times to retry command if it fails due to a
     *            exception. DeviceNotResponsiveException will be thrown if <var>retryAttempts</var>
     *            are performed without success.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public void executeShellCommand(String command, IShellOutputReceiver receiver,
            int maxTimeToOutputShellResponse, int retryAttempts) throws DeviceNotAvailableException;

    /**
     * Helper method which executes a adb shell command and returns output as a {@link String}.
     *
     * @param command the adb shell command to run
     * @return the shell output
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public String executeShellCommand(String command) throws DeviceNotAvailableException;

    /**
     * Helper method which executes a adb command as a system command.
     * <p/>
     * {@link #executeShellCommand(String)} should be used instead wherever possible, as that
     * method provides better failure detection and performance.
     *
     * @param commandArgs the adb command and arguments to run
     * @return the stdout from command. <code>null</code> if command failed to execute.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public String executeAdbCommand(String... commandArgs) throws DeviceNotAvailableException;

    /**
     * Helper method which executes a fastboot command as a system command.
     * <p/>
     * Expected to be used when device is already in fastboot mode.
     *
     * @param commandArgs the fastboot command and arguments to run
     * @return the CommandResult containing output of command
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public CommandResult executeFastbootCommand(String... commandArgs)
            throws DeviceNotAvailableException;

    /**
     * Helper method which executes a long running fastboot command as a system command.
     * <p/>
     * Identical to {@link #executeFastbootCommand(String...)} except uses a longer timeout.
     *
     * @param commandArgs the fastboot command and arguments to run
     * @return the CommandResult containing output of command
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public CommandResult executeLongFastbootCommand(String... commandArgs)
            throws DeviceNotAvailableException;

    /**
     * Get whether to use fastboot erase or fastboot format to wipe a partition on the device.
     *
     * @return {@code true} if fastboot erase will be used or {@code false} if fastboot format will
     * be used.
     * @see #fastbootWipePartition(String)
     */
    public boolean getUseFastbootErase();

    /**
     * Set whether to use fastboot erase or fastboot format to wipe a partition on the device.
     *
     * @param useFastbootErase {@code true} if fastboot erase should be used or {@code false} if
     * fastboot format should be used.
     * @see #fastbootWipePartition(String)
     */
    public void setUseFastbootErase(boolean useFastbootErase);

    /**
     * Helper method which wipes a partition for the device.
     * <p/>
     * If {@link #getUseFastbootErase()} is {@code true}, then fastboot erase will be used to wipe
     * the partition. The device must then create a filesystem the next time the device boots.
     * Otherwise, fastboot format is used which will create a new filesystem on the device.
     * <p/>
     * Expected to be used when device is already in fastboot mode.
     *
     * @param partition the partition to wipe
     * @return the CommandResult containing output of command
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public CommandResult fastbootWipePartition(String partition) throws DeviceNotAvailableException;

    /**
     * Runs instrumentation tests, and provides device recovery.
     * <p/>
     * If connection with device is lost before test run completes, and recovery succeeds, all
     * listeners will be informed of testRunFailed and "false" will be returned. The test command
     * will not be rerun.. It is left to callers to retry if necessary.
     * <p/>
     * If connection with device is lost before test run completes, and recovery fails, all
     * listeners will be informed of testRunFailed and DeviceNotAvailableException will be thrown.
     *
     * @param runner the {@IRemoteAndroidTestRunner} which runs the tests
     * @param listeners the test result listeners
     * @return <code>true</code> if test command completed. <code>false</code> if it failed to
     *         complete due to device communication exception, but recovery succeeded
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered. ie test command failed to complete and recovery failed.
     */
    public boolean runInstrumentationTests(IRemoteAndroidTestRunner runner,
            Collection<ITestRunListener> listeners) throws DeviceNotAvailableException;

    /**
     * Convenience method for performing
     * {@link #runInstrumentationTests(IRemoteAndroidTestRunner, Collection)} with one or listeners
     * passed as parameters.
     *
     * @param runner the {@IRemoteAndroidTestRunner} which runs the tests
     * @param listener the test result listener
     * @return <code>true</code> if test command completed. <code>false</code> if it failed to
     *         complete, but recovery succeeded
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered. ie test command failed to complete and recovery failed.
     */
    public boolean runInstrumentationTests(IRemoteAndroidTestRunner runner,
            ITestRunListener... listeners) throws DeviceNotAvailableException;

    /**
     * Install an Android package on device.
     *
     * @param packageFile the apk file to install
     * @param reinstall <code>true</code> if a reinstall should be performed
     * @param extraArgs optional extra arguments to pass. See 'adb shell pm install --help' for
     *            available options.
     * @return a {@link String} with an error code, or <code>null</code> if success.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public String installPackage(File packageFile, boolean reinstall, String... extraArgs)
            throws DeviceNotAvailableException;

    /**
     * Uninstall an Android package from device.
     *
     * @param packageName the Android package to uninstall
     * @return a {@link String} with an error code, or <code>null</code> if success.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public String uninstallPackage(String packageName) throws DeviceNotAvailableException;

    /**
     * Returns a mount point.
     * <p/>
     * Queries the device directly if the cached info in {@link IDevice} is not available.
     * <p/>
     * TODO: move this behavior to {@link IDevice#getMountPoint(String)}
     *
     * @param mountName the name of the mount point
     * @return the mount point or <code>null</code>
     * @see {@link IDevice#getMountPoint(String)}
     */
    public String getMountPoint(String mountName);

    /**
     * Returns a parsed version of the information in /proc/mounts on the device
     *
     * @return A {@link List<MountPointInfo>} containing the information in "/proc/mounts"
     */
    public List<MountPointInfo> getMountPointInfo() throws DeviceNotAvailableException;

    /**
     * Returns a {@link MountPointInfo} corresponding to the specified mountpoint path, or
     * <code>null</code> if that path has nothing mounted or otherwise does not appear in
     * /proc/mounts as a mountpoint.
     *
     * @return A {@link List<MountPointInfo>} containing the information in "/proc/mounts"
     * @see {@link getMountPointInfo()}
     */
    public MountPointInfo getMountPointInfo(String mountpoint) throws DeviceNotAvailableException;

    /**
     * Retrieves a bugreport from the device.
     * <p/>
     * The implementation of this is guaranteed to continue to work on a device without an sdcard
     * (or where the sdcard is not yet mounted).
     *
     * @return An {@link InputStreamSource} which will produce the bugreport contents on demand.  In
     *         case of failure, the {@code InputStreamSource} will produce an empty
     *         {@link InputStream}.
     */
    public InputStreamSource getBugreport();

    /**
     * Retrieves a file off device.
     *
     * @param remoteFilePath the absolute path to file on device.
     * @param localFile the local file to store contents in. If non-empty, contents will be
     *            replaced.
     * @return <code>true</code> if file was retrieved successfully. <code>false</code> otherwise.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public boolean pullFile(String remoteFilePath, File localFile)
            throws DeviceNotAvailableException;

    /**
     * Retrieves a file off device, stores it in a local temporary {@link File}, and returns that
     * {@code File}.
     *
     * @param remoteFilePath the absolute path to file on device.
     * @return A {@link File} containing the contents of the device file, or {@code null} if the
     *         copy failed for any reason (including problems with the host filesystem)
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public File pullFile(String remoteFilePath) throws DeviceNotAvailableException;

    /**
     * A convenience method to retrieve a file from the device's external storage, stores it in a
     * local temporary {@link File}, and return a reference to that {@code File}.
     *
     * @param remoteFilePath the path to file on device, relative to the device's external storage
     *        mountpoint
     * @return A {@link File} containing the contents of the device file, or {@code null} if the
     *         copy failed for any reason (including problems with the host filesystem)
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public File pullFileFromExternal(String remoteFilePath) throws DeviceNotAvailableException;

    /**
     * Push a file to device
     *
     * @param localFile the local file to push
     * @param deviceFilePath the remote destination absolute file path
     * @return <code>true</code> if file was pushed successfully. <code>false</code> otherwise.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public boolean pushFile(File localFile, String deviceFilePath)
            throws DeviceNotAvailableException;

    /**
     * Push file created from a string to device
     *
     * @param contents the contents of the file to push
     * @param deviceFilePath the remote destination absolute file path
     * @return <code>true</code> if string was pushed successfully. <code>false</code> otherwise.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public boolean pushString(String contents, String deviceFilePath)
            throws DeviceNotAvailableException;

    /**
     * Recursively push directories contents to device.
     *
     * @param localFile the local directory to push
     * @param deviceFilePath the remote destination absolute file path
     * @return <code>true</code> if file was pushed successfully. <code>false</code> otherwise.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public boolean pushDir(File localDir, String deviceFilePath)
            throws DeviceNotAvailableException;

    /**
     * Incrementally syncs the contents of a local file directory to device.
     * <p/>
     * Decides which files to push by comparing timestamps of local files with their remote
     * equivalents. Only 'newer' or non-existent files will be pushed to device. Thus overhead
     * should be relatively small if file set on device is already up to date.
     * <p/>
     * Hidden files (with names starting with ".") will be ignored.
     * <p/>
     * Example usage: syncFiles("/tmp/files", "/sdcard") will created a /sdcard/files directory if
     * it doesn't already exist, and recursively push the /tmp/files contents to /sdcard/files.
     *
     * @param localFileDir the local file directory containing files to recursively push.
     * @param deviceFilePath the remote destination absolute file path root. All directories in thos
     *            file path must be readable. ie pushing to /data/local/tmp when adb is not root
     *            will fail
     * @return <code>true</code> if files were synced successfully. <code>false</code> otherwise.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public boolean syncFiles(File localFileDir, String deviceFilePath)
            throws DeviceNotAvailableException;

    /**
     * Helper method to determine if file on device exists.
     *
     * @param deviceFilePath the absolute path of file on device to check
     * @return <code>true</code> if file exists, <code>false</code> otherwise.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public boolean doesFileExist(String deviceFilePath) throws DeviceNotAvailableException;

    /**
     * Helper method to determine amount of free space on device external storage.
     *
     * @return the amount of free space in KB
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public long getExternalStoreFreeSpace() throws DeviceNotAvailableException;

    /**
     * Retrieve a reference to a remote file on device.
     *
     * @param path the file path to retrieve. Can be an absolute path or path relative to '/'. (ie
     *            both "/system" and "system" syntax is supported)
     * @return the {@link IFileEntry} or <code>null</code> if file at given <var>path</var> cannot
     *         be found
     * @throws DeviceNotAvailableException
     */
    public IFileEntry getFileEntry(String path) throws DeviceNotAvailableException;

    /**
     * Deletes any accumulated logcat data.
     * <p/>
     * This is useful for cases when you want to ensure {@link ITestDevice#getLogcat()} only returns
     * log data produced after a certain point (such as after flashing a new device build, etc).
     */
    public void clearLogcat();

    /**
     * Grabs a snapshot stream of the logcat data.
     */
    public InputStreamSource getLogcat();

    /**
     * Grabs a screenshot from the device.
     *
     * @return a {@link InputStreamSource} of the screenshot in png format, or <code>null</code> if
     *         the screenshot was not successful.
     * @throws DeviceNotAvailableException
     */
    public InputStreamSource getScreenshot() throws DeviceNotAvailableException;

    /**
     * Connects to a wifi network.
     * <p/>
     * Turns on wifi and blocks until a successful connection is made to the specified wifi network.
     * adb needs to be running as root.
     *
     * @param wifiSsid the wifi ssid to connect to
     * @param wifiPsk PSK passphrase or null if unencrypted
     * @return <code>true</code> if connected to wifi network successfully. <code>false</code>
     *         otherwise
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public boolean connectToWifiNetwork(String wifiSsid, String wifiPsk)
            throws DeviceNotAvailableException;

    /**
     * Disconnects from a wifi network.
     * <p/>
     * Removes all networks from known networks list and disables wifi.
     *
     * @return <code>true</code> if disconnected from wifi network successfully. <code>false</code>
     *         if disconnect failed.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public boolean disconnectFromWifi() throws DeviceNotAvailableException;

    /**
     * Gets the device's IP address.
     *
     * @return the device's IP address, or <code>null</code> if device has no IP address
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public String getIpAddress() throws DeviceNotAvailableException;

    /**
     * Attempt to dismiss any error dialogs currently displayed on device UI.
     *
     * @return <code>true</code> if no dialogs were present or dialogs were successfully cleared.
     *         <code>false</code> otherwise.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *             recovered.
     */
    public boolean clearErrorDialogs() throws DeviceNotAvailableException;

    /**
     * Reboots the device into bootloader mode.
     * <p/>
     * Blocks until device is in bootloader mode.
     *
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public void rebootIntoBootloader() throws DeviceNotAvailableException;

    /**
     * Reboots the device into adb mode.
     * <p/>
     * Blocks until device becomes available.
     *
     * @throws DeviceNotAvailableException if device is not available after reboot
     */
    public void reboot() throws DeviceNotAvailableException;

    /**
     * Reboots the device into adb recovery mode.
     * <p/>
     * Blocks until device enters recovery
     *
     * @throws DeviceNotAvailableException if device is not available after reboot
     */
    public void rebootIntoRecovery() throws DeviceNotAvailableException;

    /**
     * An alternate to {@link #reboot()} that only blocks until device is online ie visible to adb.
     *
     * @throws DeviceNotAvailableException if device is not available after reboot
     */
    public void rebootUntilOnline() throws DeviceNotAvailableException;

    /**
     * Issues a command to reboot device and returns on command complete and when device is no
     * longer visible to adb.
     *
     * @throws DeviceNotAvailableException
     */
    public void nonBlockingReboot() throws DeviceNotAvailableException;

    /**
     * Turns on adb root.
     * <p/>
     * Enabling adb root may cause device to disconnect from adb. This method will block until
     * device is available.
     *
     * @return <code>true</code> if successful.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public boolean enableAdbRoot() throws DeviceNotAvailableException;

    /**
     * Get the device's state.
     */
    public TestDeviceState getDeviceState();

    /**
     * Encrypts the device.
     * <p/>
     * Encrypting the device may be done inplace or with a wipe.  Inplace encryption will not wipe
     * any data on the device but normally takes a couple orders of magnitude longer than the wipe.
     * <p/>
     * This method will reboot the device if it is not already encrypted and will block until device
     * is online.  Also, it will not decrypt the device after the reboot.  Therefore, the device
     * might not be fully booted and/or ready to be tested when this method returns.
     *
     * @param inplace if the encryption process should take inplace and the device should not be
     * wiped.
     * @return <code>true</code> if successful.
     * @throws DeviceNotAvailableException if device is not available after reboot.
     * @throws UnsupportedOperationException if encryption is not supported on the device.
     */
    public boolean encryptDevice(boolean inplace) throws DeviceNotAvailableException,
            UnsupportedOperationException;

    /**
     * Unencrypts the device.
     * <p/>
     * Unencrypting the device may cause device to be wiped and may reboot device. This method will
     * block until device is available and ready for testing.  Requires fastboot inorder to wipe the
     * userdata partition.
     *
     * @return <code>true</code> if successful.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     * @throws UnsupportedOperationException if encryption is not supported on the device.
     */
    public boolean unencryptDevice() throws DeviceNotAvailableException,
            UnsupportedOperationException;

    /**
     * Unlocks the device if the device is in an encrypted state.
     * </p>
     * This method may restart the framework but will not call {@link #postBootSetup()}. Therefore,
     * the device might not be fully ready to be tested when this method returns.
     *
     * @return <code>true</code> if successful or if the device is unencrypted.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     * @throws UnsupportedOperationException if encryption is not supported on the device.
     */
    public boolean unlockDevice() throws DeviceNotAvailableException,
            UnsupportedOperationException;

    /**
     * Returns if the device is encrypted.
     *
     * @return <code>true</code> if the device is encrypted.
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public boolean isDeviceEncrypted() throws DeviceNotAvailableException;

    /**
     * Returns if encryption is supported on the device.
     *
     * @return <code>true</code> if the device supports encryption.
     * @throws DeviceNotAvailableException
     */
    public boolean isEncryptionSupported() throws DeviceNotAvailableException;

    /**
     * Waits for the device to be responsive and available for testing.
     *
     * @param waitTime the time in ms to wait
     * @throws DeviceNotAvailableException if device is still unresponsive after waitTime expires.
     */
    public void waitForDeviceAvailable(final long waitTime) throws DeviceNotAvailableException;

    /**
     * Waits for the device to be responsive and available for testing.  Uses default timeout.
     *
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public void waitForDeviceAvailable() throws DeviceNotAvailableException;

    /**
     * Blocks until device is visible via adb.
     * <p/>
     * Note the device may not necessarily be responsive to commands on completion. Use
     * {@link #waitForDeviceAvailable()} instead.
     *
     * @param waitTime the time in ms to wait
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public void waitForDeviceOnline(final long waitTime) throws DeviceNotAvailableException;

    /**
     * Blocks until device is visible via adb.  Uses default timeout
     * <p/>
     * Note the device may not necessarily be responsive to commands on completion. Use
     * {@link #waitForDeviceAvailable()} instead.
     *
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public void waitForDeviceOnline() throws DeviceNotAvailableException;

    /**
     * Blocks for the device to be not available ie missing from adb
     *
     * @param waitTime the time in ms to wait
     * @return <code>true</code> if device becomes not available before time expires.
     *         <code>false</code> otherwise
     */
    public boolean waitForDeviceNotAvailable(final long waitTime);

    /**
     * Blocks for the device to be in the 'adb recovery' state (note this is distinct from
     * {@link IDeviceRecovery}).
     *
     * @param waitTime the time in ms to wait
     * @return <code>true</code> if device boots into recovery before time expires.
     *         <code>false</code> otherwise
     */
    public boolean waitForDeviceInRecovery(final long waitTime);

    /**
     * Perform instructions to configure device for testing that after every boot.
     * <p/>
     * Should be called after device is fully booted/available
     * <p/>
     * In normal circumstances this method doesn't need to be called explicitly, as
     * implementations should perform these steps automatically when performing a reboot.
     * <p/>
     * Where it may need to be called is when device reboots due to other events (eg when a
     * fastboot update command has completed)
     *
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public void postBootSetup() throws DeviceNotAvailableException;

    /**
     * @return <code>true</code> if device is connected to adb-over-tcp, <code>false</code>
     * otherwise.
     */
    public boolean isAdbTcp();


    /**
     * @return <code>true</code> if device currently has adb root, <code>false</code> otherwise.
     *
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     * recovered.
     */
    public boolean isAdbRoot() throws DeviceNotAvailableException;

    /**
     * Switch device to adb-over-tcp mode.
     *
     * @return the tcp serial number or <code>null</code> if device could not be switched
     * @throws DeviceNotAvailableException
     */
    public String switchToAdbTcp() throws DeviceNotAvailableException;

    /**
     * Switch device to adb over usb mode.
     *
     * @return <code>true</code> if switch was successful, <code>false</code> otherwise.
     * @throws DeviceNotAvailableException
     */
    public boolean switchToAdbUsb() throws DeviceNotAvailableException;

    /**
     * Set the {@link TestDeviceOptions} for the device
     */
    public void setOptions(TestDeviceOptions options);

    /**
     * Fetch the test options for the device.
     *
     * @return {@link TestDeviceOptions} related to the device under test.
     */
    public TestDeviceOptions getOptions();

    /**
     * Fetch the package names installed on the device.
     *
     * @return {@link Set} of {@link String} package names currently installed on the device.
     * @throws DeviceNotAvailableException
     */
    public Set<String> getInstalledPackageNames() throws DeviceNotAvailableException;

    /**
     * Fetch the package names installed on the device.
     *
     * @return {@link List} of {@link PartitionInfo} partitions of the internal memory if the device.
     * @throws DeviceNotAvailableException
     */
    public List<PartitionInfo> getPartitionInfo() throws DeviceNotAvailableException;

}
