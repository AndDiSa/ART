/*
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
package de.anddisa.remotebackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;

import com.android.ddmlib.FileListingService.FileEntry;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.NullOutputReceiver;

import de.anddisa.adb.device.DeviceManager;
import de.anddisa.adb.device.DeviceNotAvailableException;
import de.anddisa.adb.device.DeviceSelectionOptions;
import de.anddisa.adb.device.IDeviceManager;
import de.anddisa.adb.device.ITestDevice;
import de.anddisa.adb.device.ITestDevice.MountPointInfo;
import de.anddisa.adb.device.ITestDevice.PartitionInfo;
/**
 * 
 * This class wraps adb commands to "high level" commands and manages the
 * communication with the remote device
 *
 */
public class AdbWrapper {
	
	// -----------------------------------------------------------------------------
	// some utilities
	// -----------------------------------------------------------------------------

	/**
	 * sub class which receives data from remote and writes it to the
	 * local file system
	 *
	 */
	public class FileReceiver implements IShellOutputReceiver {

		private FileOutputStream fos = null;
		boolean isCancelled = false;
		private long size = 0;
		private long block = 0;
		
		public FileReceiver(String fileName) {
			super();
			try {
				this.fos = new FileOutputStream(fileName);
			} catch (FileNotFoundException e) {
				isCancelled = true;
			}
		}
		
		public void addOutput(byte[] data, int offset, int length) {
			try {
				fos.write(data, offset, length);
				size += length;
				block++;
				// some debugging (TODO: remove)
				if (length != 4096) {
					System.out.println("block:" + block + " size:" + length);
				}
			} catch (IOException e) {
				isCancelled = true;
			}
		}

		public void flush() {
			try {
				fos.flush();
				System.out.println("blocks:" + block);
				System.out.println("size:" + size);
				fos.close();
			} catch (IOException e) {
				isCancelled = true;
			}
		}

		public boolean isCancelled() {
			return isCancelled;
		}
	}

	/**
	 * calculates the md5 sum for the file passed as srcFileName and
	 * compares it to the md5sum stored in md5FileName
	 * 
	 * @param srcFileName {@link String} path to the file for which md5 needs to be calculated
	 * @param md5FileName {@link String} path to the file which contains the md5sum to be matched
	 * @return {@link Boolean} true if md5sum matches, false otherwise
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static boolean compareMD5(String srcFileName, String md5FileName) throws IOException, NoSuchAlgorithmException {
		String md5sum = md5sum(srcFileName);
        File f = new File(md5FileName);

        //
        // md5sum is written within the first 32 byte, ignore the rest
        //
        InputStream is = new FileInputStream(f);
        byte[] buffer = new byte[32];
        is.read(buffer);
        String s = new String(buffer);
        is.close();

        boolean result = md5sum.equals(s);
        if (!result) {
        	// print out some error message
        	System.err.println(srcFileName + "(" + md5sum + ") <-> " + md5FileName + "(" + s + ")");
        }
        return result;
	}

	/**
	 * calculates the md5 sum of a file
	 * 
	 * @param fileName {@link String} path to the file for which md5 needs to be calculated
	 * @return md5sum {@link String} of the file
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static String md5sum(String fileName) throws IOException, NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");

		// read file and calculate md5
		File f = new File(fileName);
        InputStream is = new FileInputStream(f);
        byte[] buffer = new byte[8192];
        int read = 0;
        while( (read = is.read(buffer)) > 0)
                md.update(buffer, 0, read);
        is.close();

        // format md5
        byte[] md5 = md.digest();
        BigInteger bi = new BigInteger(1, md5);
        
        return bi.toString(16);	// print out as HEX number
	}
	
	// -----------------------------------------------------------------------------
	// main class
	// -----------------------------------------------------------------------------
	
	private static long TIME0UT = 5000;
	
	private static IDeviceManager deviceManager = DeviceManager.getInstance();
	private String ddmsParentLocation = null;
	private ITestDevice selectedDevice;
	private boolean adbRunsAsRoot;

	/**
	 * constructor
	 * 
	 * @param ddmsParentLocation {@link String} path to adb binaries
	 */
	public AdbWrapper(String ddmsParentLocation) {
		this.ddmsParentLocation = ddmsParentLocation;
		init();
	}

	/**
	 * initialize the wrapper
	 */
	private void init() {
		// [try to] ensure ADB is running
		// in the new SDK, adb is in the platform-tools, but when run from the  command line
		// in the Android source tree, then adb is next to ddms.
		String adbLocation;

		if (ddmsParentLocation != null && ddmsParentLocation.length() != 0) {
			// check if there's a platform-tools folder
			File platformTools = new File(new File(ddmsParentLocation).getParent(), "platform-tools"); //$NON-NLS-1$
			if (platformTools.isDirectory()) {
				adbLocation = platformTools.getAbsolutePath() + File.separator + "adb"; //$NON-NLS-1$
			} else {
				adbLocation = ddmsParentLocation + File.separator + "adb"; //$NON-NLS-1$
			}
		} else {
			adbLocation = "adb"; //$NON-NLS-1$
		}
		deviceManager.init(adbLocation);
	}

	/**
	 * returns a collection of available devices
	 * 
	 * @return {@link Collection}
	 */
	public Collection<String> getDevices() {
		return deviceManager.getAvailableDevices();
	}

	/**
	 * returns interface to the current / selected device
	 * 
	 * @return {@link ITestDevice}
	 */
	public ITestDevice getCurrentDevice() {
		return selectedDevice;
	}

	/**
	 * selects the device with the id passed as current
	 * 
	 * @param deviceId {@link String}
	 */
	public void selectDevice(String deviceId) {
		if (deviceId != null) {
			DeviceSelectionOptions dso = new DeviceSelectionOptions();
			dso.addSerial(deviceId);
			selectedDevice = deviceManager.allocateDevice(TIME0UT, dso);
		} else {
			selectedDevice = deviceManager.allocateDevice();
		}
		checkAdbRunsAsRoot();
	}

	/**
	 * check whether the adb shell runs as user or as root
	 */
	private void checkAdbRunsAsRoot() {
		try {
			adbRunsAsRoot = selectedDevice.isAdbRoot();
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * check whether we need to wrap shell commands as su commands
	 */
	private boolean isSuNeeded() {
		return !adbRunsAsRoot;
	}

	/**
	 * wraps a command with su command, if needed
	 * 
	 * @param command {@link String} to be executes
	 * 
	 * @return {@link String} command to be executed as root
	 */
	private String getRootExecutableCommand(String command) {
		if (isSuNeeded()) {
			return "su -c \"" + command + "\"";
		}
		return command;
	}

	// -----------------------------------------------------------------------------
	// methods related to tar
	// -----------------------------------------------------------------------------
	
	/**
	 * gets the content of the system partition as tar file
	 * 
	 * @param toFilePath {@link String} where the file shall be stored locally
	 * 
	 * @return {@link Boolean} true if the transfer succeeded, false otherwise
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public boolean getSytemPartitionAsTar(String toFilePath) throws NoSuchAlgorithmException, IOException {
		final String SYSTEM = "system";
		final String SYSTEM_TAR = SYSTEM + ".tar";
		final String SYSTEM_TAR_MD5 = SYSTEM_TAR + ".md5";
		
		boolean result = true;
		
		result &= getFileSystemAsTar(toFilePath, "/" + SYSTEM, SYSTEM);
		result &= getTarFileMD5("/" + SYSTEM, toFilePath + "/" + SYSTEM_TAR_MD5);
		result &= AdbWrapper.compareMD5(toFilePath + "/" + SYSTEM_TAR, toFilePath + "/" + SYSTEM_TAR_MD5);
		return result;
	}

	/**
	 * gets the content of the data partition as tar file
	 * 
	 * @param toFilePath {@link String} where the file shall be stored locally
	 * 
	 * @return {@link Boolean} true if the transfer succeeded, false otherwise
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * 
	 * TODO: md5sum fails when backup is done on a running system ...
	 * 		(obviously, but how to handle ...) 
	 */
	public boolean getDataPartitionAsTar(String toFilePath) throws NoSuchAlgorithmException, IOException {
		final String DATA = "data";
		final String DATA_TAR = DATA + ".tar";
		final String DATA_TAR_MD5 = DATA_TAR + ".md5";

		boolean result = true;
		
		result &= getFileSystemAsTar(toFilePath, "/" + DATA, DATA);
		result &= getTarFileMD5("/" + DATA, toFilePath + "/" + DATA_TAR_MD5);
		result &= AdbWrapper.compareMD5(toFilePath + "/" + DATA_TAR, toFilePath + "/" + DATA_TAR_MD5);

		return result;
	}

	/**
	 * gets the content of the sd-ext partition as tar file
	 * 
	 * @param toFilePath {@link String} where the file shall be stored locally
	 * 
	 * @return {@link Boolean} true if the transfer succeeded, false otherwise
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * 
	 * TODO: md5sum may fail when backup is done on a running system ...
	 * 		(obviously, but how to handle ...) 
	 */
	public boolean getExtPartitionAsTar(String toFilePath) throws NoSuchAlgorithmException, IOException {
		final String SD_EXT = "sd-ext";
		final String SD_EXT_TAR = SD_EXT + ".tar";
		final String SD_EXT_TAR_MD5 = SD_EXT_TAR + ".md5";

		boolean result = true;
		
		result &= getFileSystemAsTar(toFilePath, "/" + SD_EXT, SD_EXT);
		result &= getTarFileMD5("/" + SD_EXT, toFilePath + "/" + SD_EXT_TAR_MD5);
		result &= AdbWrapper.compareMD5(toFilePath + "/" + SD_EXT_TAR, toFilePath + "/" + SD_EXT_TAR_MD5);

		return result;
	}

	/**
	 * gets the content of the sdcard partition as tar file
	 * 
	 * @param toFilePath {@link String} where the file shall be stored locally
	 * 
	 * @return {@link Boolean} true if the transfer succeeded, false otherwise
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * 
	 * TODO: md5sum may fail when backup is done on a running system ...
	 * 		(obviously, but how to handle ...) 
	 */
	public boolean getSdcardPartitionAsTar(String toFilePath) throws NoSuchAlgorithmException, IOException {
		final String SDCARD = "sdcard";
		final String SDCARD_TAR = SDCARD + ".tar";
		final String SDCARD_TAR_MD5 = SDCARD_TAR + ".md5";

		boolean result = true;
		
		result &= getFileSystemAsTar(toFilePath, "/" + SDCARD, SDCARD);
		result &= getTarFileMD5("/" + SDCARD, toFilePath + "/" + SDCARD_TAR_MD5);
		result &= AdbWrapper.compareMD5(toFilePath + "/" + SDCARD_TAR, toFilePath + "/" + SDCARD_TAR_MD5);

		return result;
	}

	/**
	 * gets the content of a file system as tar file
	 * 
	 * @param toFilePath {@link String} where the file shall be stored locally
	 * @param startDirectory {@link String} root directory where to start
	 * @param tarFileName {@link String} name of the tar file to be used
	 * 
	 * @return {@link Boolean} true if the transfer succeeded, false otherwise
	 * 
	 */
	private boolean getFileSystemAsTar(String toFilePath, String startDirectory, String tarFileName) {
		boolean result = true;

		final String createFifoString = "busybox rm -f /cache/myfifo; busybox mkfifo /cache/myfifo";
		final String tarString = "busybox stty raw; busybox tar cvf /cache/myfifo " + startDirectory;
		final String catString = "busybox stty raw; busybox cat /cache/myfifo";
		
		//
		// create a fifo to transfer data from remote to local
		//
		try {
			selectedDevice.executeShellCommand(getRootExecutableCommand(createFifoString), NullOutputReceiver.getReceiver());
		} catch (DeviceNotAvailableException e) {
			result = false;
		}
		
		//
		// run the tar command
		//
		Runnable tarCommand = new Runnable() {		
			@Override
			public void run() {
				try {
					selectedDevice.executeShellCommand(getRootExecutableCommand(tarString), NullOutputReceiver.getReceiver());					
				} catch (DeviceNotAvailableException e) {
					e.printStackTrace();
				}
			}
		};
		
		final FileReceiver tarFileReceiver = new FileReceiver(toFilePath + "/" + tarFileName + ".tar");
		
		//
		// run the cat command
		//
		Runnable catCommand = new Runnable() {		
			@Override
			public void run() {
				try {
					selectedDevice.executeShellCommand(getRootExecutableCommand(catString), tarFileReceiver);
				} catch (DeviceNotAvailableException e) {
					e.printStackTrace();
				}
			}
		};

		//
		// start both threads and wait for the end
		//
		Thread catCommandExecutor = new Thread(catCommand);
		catCommandExecutor.start();

		Thread tarCommandExecutor = new Thread(tarCommand);
		tarCommandExecutor.start();

		try {
			catCommandExecutor.join();
		} catch (InterruptedException e) {
			result = false;
		}
		try {
			tarCommandExecutor.join();
		} catch (InterruptedException e) {
			result = false;
		}

		return result;
	}
	
	/**
	 * gets the md5sum of a file system
	 * 
	 * @param rootFileName {@link String} rootFileName to get the md5sum for
	 * @param md5FileName {@link String} filename the md5sum should be stored in
	 * 
	 * @return {@link Boolean} true if succeeded, false otherwise
	 */
	private boolean getTarFileMD5(String rootFileName, String md5FileName) {
		boolean result = true;

		final String md5sumString = "busybox tar cf - " + rootFileName + " | md5sum";

		try {
			selectedDevice.executeShellCommand(getRootExecutableCommand(md5sumString), new FileReceiver(md5FileName));
		} catch (DeviceNotAvailableException e) {
			result = false;
		}
		return result;
	}
	
	/**
	 * gets the system partition as an image from remote
	 * 
	 * @param imageFileName {@link String} filename the partition should be stored in
	 * 
	 * @return {@link Boolean} true if succeeded, false otherwise
	 */
	public boolean getSystemPartitionAsImage(String imageFileName) {
		PartitionInfo systemPartition = selectedDevice.getSystemPartition();
		if (systemPartition != null && systemPartition.deviceName != null && !"".equals(systemPartition.deviceName)) {
			return getFileSystemAsImage(systemPartition.deviceName, imageFileName);
		} else {
			return false;
		}
	}

	/**
	 * gets the data partition as an image from remote
	 * 
	 * @param imageFileName {@link String} filename the partition should be stored in
	 * 
	 * @return {@link Boolean} true if succeeded, false otherwise
	 */
	public boolean getDataPartitionAsImage(String imageFileName) {
		PartitionInfo dataPartition = selectedDevice.getDataPartition();
		if (dataPartition != null && dataPartition.deviceName != null && !"".equals(dataPartition.deviceName)) {
			return getFileSystemAsImage(dataPartition.deviceName, imageFileName);
		} else {
			return false;
		}
	}

	/**
	 * gets the cache partition as an image from remote
	 * 
	 * @param imageFileName {@link String} filename the partition should be stored in
	 * 
	 * @return {@link Boolean} true if succeeded, false otherwise
	 */
	public boolean getCachePartitionAsImage(String imageFileName) {
		PartitionInfo cachePartition = selectedDevice.getCachePartition();
		if (cachePartition != null && cachePartition.deviceName != null && !"".equals(cachePartition.deviceName)) {
			return getFileSystemAsImage(cachePartition.deviceName, imageFileName);
		} else {
			return false;
		}
	}

	/**
	 * gets the boot partition as an image from remote
	 * 
	 * @param imageFileName {@link String} filename the partition should be stored in
	 * 
	 * @return {@link Boolean} true if succeeded, false otherwise
	 */
	public boolean getBootPartitionAsImage(String imageFileName) {
		PartitionInfo bootPartition = selectedDevice.getBootPartition();
		if (bootPartition != null && bootPartition.deviceName != null && !"".equals(bootPartition.deviceName)) {
			return getFileSystemAsImage(bootPartition.deviceName, imageFileName);
		} else {
			return false;
		}
	}

	/**
	 * gets the recovery partition as an image from remote
	 * 
	 * @param imageFileName {@link String} filename the partition should be stored in
	 * 
	 * @return {@link Boolean} true if succeeded, false otherwise
	 */
	public boolean getRecoveryPartitionAsImage(String imageFileName) {
		PartitionInfo recoveryPartition = selectedDevice.getRecoveryPartition();
		if (recoveryPartition != null && recoveryPartition.deviceName != null && !"".equals(recoveryPartition.deviceName)) {
			return getFileSystemAsImage(recoveryPartition.deviceName, imageFileName);
		} else {
			return false;
		}
	}

	/**
	 * gets the misc partition as an image from remote
	 * 
	 * @param imageFileName {@link String} filename the partition should be stored in
	 * 
	 * @return {@link Boolean} true if succeeded, false otherwise
	 */
	public boolean getMiscPartitionAsImage(String imageFileName) {
		PartitionInfo miscPartition = selectedDevice.getMiscPartition();
		if (miscPartition != null && miscPartition.deviceName != null && !"".equals(miscPartition.deviceName)) {
			return getFileSystemAsImage(miscPartition.deviceName, imageFileName);
		} else {
			return false;
		}
	}

	/**
	 * gets the partition with the given name as an image from remote
	 * 
	 * @param partitionName {@link String} name of the partition to be read
	 * @param imageFileName {@link String} filename the partition should be stored in
	 * 
	 * @return {@link Boolean} true if succeeded, false otherwise
	 */
	public boolean getPartitionAsImage(String partitionName, String imageFileName) {
		PartitionInfo selectedPartition = selectedDevice.getPartition(partitionName);
		if (selectedPartition != null && selectedPartition.deviceName != null && !"".equals(selectedPartition.deviceName)) {
			return getFileSystemAsImage(selectedPartition.deviceName, imageFileName);
		} else {
			return false;
		}
	}

	/**
	 * gets the md5sum of a partition
	 * 
	 * @param partitionName {@link String} partitionName to get the md5sum for
	 * @param md5FileName {@link String} filename the md5sum should be stored in
	 * 
	 * @return {@link Boolean} true if succeeded, false otherwise
	 */
	public boolean getPartitionMD5(String partitionName, String md5FileName) {
		PartitionInfo selectedPartition = selectedDevice.getPartition(partitionName);
		if (selectedPartition != null && selectedPartition.deviceName != null && !"".equals(selectedPartition.deviceName)) {
			return getFileSystemImageMD5(selectedPartition.deviceName, md5FileName);
		} else {
			return false;
		}
	}
	
	/**
	 * gets the content of a file system as an image
	 * 
	 * @param partitionDefinition {@link String}
	 * @param iamgeFileName {@link String} name of the image file
	 * 
	 * @return {@link Boolean} true if the transfer succeeded, false otherwise
	 * 
	 */
	private boolean getFileSystemAsImage(String partitionDefinition, String imageFileName) {
		boolean result = true;

		final String createFifoString = "busybox rm -f /cache/myfifo; busybox mkfifo /cache/myfifo";
		final String dumpString = "busybox stty raw; dd if=" + partitionDefinition + " of=/cache/myfifo bs=4096";
		final String catString = "busybox stty raw; busybox cat /cache/myfifo";

		try {
			selectedDevice.executeShellCommand(getRootExecutableCommand(createFifoString), NullOutputReceiver.getReceiver(), 0, 1);
		} catch (DeviceNotAvailableException e) {
			result = false;
		}

		Runnable dumpCommand = new Runnable() {		
			@Override
			public void run() {
				try {
					selectedDevice.executeShellCommand(getRootExecutableCommand(dumpString), NullOutputReceiver.getReceiver(), 0, 1);					
				} catch (DeviceNotAvailableException e) {
					e.printStackTrace();
				}
			}
		};
		
		final FileReceiver imageFileReceiver = new FileReceiver(imageFileName);
		
		Runnable catCommand = new Runnable() {		
			@Override
			public void run() {
				try {
					selectedDevice.executeShellCommand(getRootExecutableCommand(catString), imageFileReceiver, 0, 1);
				} catch (DeviceNotAvailableException e) {
					e.printStackTrace();
				}
			}
		};

		Thread catCommandExecutor = new Thread(catCommand);
		catCommandExecutor.start();

		Thread dumpCommandExecutor = new Thread(dumpCommand);
		dumpCommandExecutor.start();

		try {
			dumpCommandExecutor.join();
		} catch (InterruptedException e) {
			result = false;
		}
		try {
			catCommandExecutor.join();
		} catch (InterruptedException e) {
			result = false;
		}
		return result;
	}

	/**
	 * gets the md5sum of a partition
	 * 
	 * @param partitionDefinition {@link String} partitionDefinition to get the md5sum for
	 * @param md5FileName {@link String} filename the md5sum should be stored in
	 * 
	 * @return {@link Boolean} true if succeeded, false otherwise
	 */
	private boolean getFileSystemImageMD5(String partitionDefinition, String md5FileName) {
		boolean result = true;

		final String md5sumString = "busybox md5sum " + partitionDefinition;

		try {
			selectedDevice.executeShellCommand(getRootExecutableCommand(md5sumString), new FileReceiver(md5FileName));
		} catch (DeviceNotAvailableException e) {
			result = false;
		}
		return result;
	}

	// -----------------------------------------------------------------------------
	// standard adb functions
	// -----------------------------------------------------------------------------


	/**
	 * pulls a file from remote
	 * 
	 * @param from {@link String}
	 * @param to {@link String}
	 * @return {@link Boolean}
	 */
	public boolean pullFile(String from, String to) {
		boolean result = false;
		try {
			File localFile = new File(to);
			result = selectedDevice.pullFile(from, localFile);
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * pushes a file to remote
	 * 
	 * @param from {@link String}
	 * @param to {@link String}
	 * @return {@link Boolean}
	 */
	public boolean pushFile(String from, String to) {
		boolean result = false;
		try {
			File localFile = new File(from);
			result = selectedDevice.pushFile(localFile, to);
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * pushes a directory to remote
	 * 
	 * @param from {@link String}
	 * @param to {@link String}
	 * @return {@link Boolean}
	 */
	public boolean pushDirectory(String from, String to) {
		boolean result = false;
		try {
			File localFile = new File(from);
			result = selectedDevice.pushDir(localFile, to);
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * syncs files between local and remote directory
	 * 
	 * @param from {@link String}
	 * @param to {@link String}
	 * @return {@link Boolean}
	 */
	public boolean syncFiles(String localDir, String remotePath) {
		boolean result = false;
		try {
			File localFileDir = new File(localDir);
			result = selectedDevice.syncFiles(localFileDir, remotePath);
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}


	/**
	 * returns a list of mount points
	 * 
	 * @return {@link List}
	 */
	public List<MountPointInfo> getRootDirectoryEntries() {
		List<MountPointInfo> result = null;
		try {
			result = selectedDevice.getMountPointInfo();
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * returns an array of root directory entries
	 * 
	 * @return {@link FileEntry}
	 */
	public FileEntry[] getRootFileEntries() {
		FileEntry root = selectedDevice.getIDevice().getFileListingService().getRoot();
		return getDirectoryEntries(root);
	}

	/**
	 * returns an array of sub-directories for the given directory
	 * @param {@link FileEntry}
	 * @return {@link FileEntry}
	 */
	public FileEntry[] getDirectoryEntries(FileEntry fileEntry) {		
		FileEntry[] files = selectedDevice.getIDevice().getFileListingService().getChildren(fileEntry, true, null);
		return files;
	}

	/**
	 * executes a shell command on the current device
	 * 
	 * @param command {@link Sting} to be executed
	 * @param outputReceiver {@link IShellOutputReceiver} receiver of the command result
	 */
	public void executeShellCommand(String command, IShellOutputReceiver outputReceiver) {
		try {
			selectedDevice.executeShellCommand(command, outputReceiver);
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * reboot current device into recovery
	 * 
	 */
	public void rebootIntoRecovery() {
		try {
			selectedDevice.rebootIntoRecovery();
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * reboot current device into bootloader
	 * 
	 */
	public void rebootIntoBootloader() {
		try {
			selectedDevice.rebootIntoBootloader();
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * reboot current device
	 * 
	 */
	public void reboot() {
		try {
			selectedDevice.reboot();
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
