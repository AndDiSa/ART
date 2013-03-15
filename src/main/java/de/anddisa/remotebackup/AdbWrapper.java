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

public class AdbWrapper {
	
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

	public static boolean compareMD5(String srcFileName, String md5FileName) throws IOException, NoSuchAlgorithmException {
		String md5sum = md5sum(srcFileName);
        File f = new File(md5FileName);
        InputStream is = new FileInputStream(f);
        byte[] buffer = new byte[32];
        is.read(buffer);
        String s = new String(buffer);
        is.close();
        boolean result = md5sum.equals(s);
        if (!result) {
        	System.err.println(srcFileName + "(" + md5sum + ") <-> " + md5FileName + "(" + s + ")");
        }
        return result;
	}
	
	public static String md5sum(String fileName) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        File f = new File(fileName);
        InputStream is = new FileInputStream(f);
        byte[] buffer = new byte[8192];
        int read = 0;
        while( (read = is.read(buffer)) > 0)
                md.update(buffer, 0, read);
        byte[] md5 = md.digest();
        BigInteger bi = new BigInteger(1, md5);
        is.close();
        return bi.toString(16);	
	}
	
	private static long TIME0UT = 5000;
	
	private static IDeviceManager deviceManager = DeviceManager.getInstance();
	private String ddmsParentLocation = null;
	private ITestDevice selectedDevice;
	private boolean adbRunsAsRoot;
	
	public AdbWrapper(String ddmsParentLocation) {
		this.ddmsParentLocation = ddmsParentLocation;
		init();
	}
	
	private void init() {
		// [try to] ensure ADB is running
		// in the new SDK, adb is in the platform-tools, but when run from the
		// command line
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

	public Collection<String> getDevices() {
		return deviceManager.getAvailableDevices();
	}

	public ITestDevice getCurrentDevice() {
		return selectedDevice;
	}

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

	private void checkAdbRunsAsRoot() {
		try {
			adbRunsAsRoot = selectedDevice.isAdbRoot();
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private boolean isSuNeeded() {
		return !adbRunsAsRoot;
	}

	private String getRootExecutableCommand(String command) {
		if (isSuNeeded()) {
			return "su -c \"" + command + "\"";
		}
		return command;
	}
	
	public boolean getSytemPartitionAsTar(String toFilePath) {
		return getFileSystemAsTar(toFilePath, "/system", "system");
	}

	public boolean getDataPartitionAsTar(String toFilePath) {
		return getFileSystemAsTar(toFilePath, "/data", "data");
	}

	public boolean getExtPartitionAsTar(String toFilePath) {
		return getFileSystemAsTar(toFilePath, "/sd-ext", "sd-ext");
	}

	private boolean getFileSystemAsTar(String toFilePath, String startDirectory, String tarFileName) {
		boolean result = true;

		final String createFifoString = "busybox rm -f /cache/myfifo; busybox mkfifo /cache/myfifo";
		final String tarString = "busybox stty raw; busybox tar cvf /cache/myfifo " + startDirectory;
		final String catString = "busybox stty raw; busybox cat /cache/myfifo";

		
/*
 		G1 recovery mode
 		
		final String createFifoString = "/sbin/busybox rm -f /cache/myfifo; /sbin/busybox mkfifo /cache/myfifo";
		final String tarString = "/sbin/busybox stty raw; /sbin/busybox tar cvf /cache/myfifo " + startDirectory;
		final String catString = "/sbin/busybox stty raw; /sbin/busybox cat /cache/myfifo";
*/
		
		try {
			selectedDevice.executeShellCommand(getRootExecutableCommand(createFifoString), NullOutputReceiver.getReceiver());
		} catch (DeviceNotAvailableException e) {
			result = false;
		}

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

	public boolean getSystemPartitionAsImage(String imageFileName) {
		PartitionInfo systemPartition = selectedDevice.getSystemPartition();
		if (systemPartition != null && systemPartition.deviceName != null && !"".equals(systemPartition.deviceName)) {
			return getFileSystemAsImage(systemPartition.deviceName, imageFileName);
		} else {
			return false;
		}
	}

	public boolean getDataPartitionAsImage(String imageFileName) {
		PartitionInfo dataPartition = selectedDevice.getDataPartition();
		if (dataPartition != null && dataPartition.deviceName != null && !"".equals(dataPartition.deviceName)) {
			return getFileSystemAsImage(dataPartition.deviceName, imageFileName);
		} else {
			return false;
		}
	}

	public boolean getCachePartitionAsImage(String imageFileName) {
		PartitionInfo cachePartition = selectedDevice.getCachePartition();
		if (cachePartition != null && cachePartition.deviceName != null && !"".equals(cachePartition.deviceName)) {
			return getFileSystemAsImage(cachePartition.deviceName, imageFileName);
		} else {
			return false;
		}
	}

	public boolean getBootPartitionAsImage(String imageFileName) {
		PartitionInfo bootPartition = selectedDevice.getBootPartition();
		if (bootPartition != null && bootPartition.deviceName != null && !"".equals(bootPartition.deviceName)) {
			return getFileSystemAsImage(bootPartition.deviceName, imageFileName);
		} else {
			return false;
		}
	}

	public boolean getRecoveryPartitionAsImage(String imageFileName) {
		PartitionInfo recoveryPartition = selectedDevice.getRecoveryPartition();
		if (recoveryPartition != null && recoveryPartition.deviceName != null && !"".equals(recoveryPartition.deviceName)) {
			return getFileSystemAsImage(recoveryPartition.deviceName, imageFileName);
		} else {
			return false;
		}
	}

	public boolean getMiscPartitionAsImage(String imageFileName) {
		PartitionInfo miscPartition = selectedDevice.getMiscPartition();
		if (miscPartition != null && miscPartition.deviceName != null && !"".equals(miscPartition.deviceName)) {
			return getFileSystemAsImage(miscPartition.deviceName, imageFileName);
		} else {
			return false;
		}
	}

	public boolean getPartitionAsImage(String partitionName, String imageFileName) {
		PartitionInfo selectedPartition = selectedDevice.getPartition(partitionName);
		if (selectedPartition != null && selectedPartition.deviceName != null && !"".equals(selectedPartition.deviceName)) {
			return getFileSystemAsImage(selectedPartition.deviceName, imageFileName);
		} else {
			return false;
		}
	}

	public boolean getPartitionMD5(String partitionName, String md5FileName) {
		PartitionInfo selectedPartition = selectedDevice.getPartition(partitionName);
		if (selectedPartition != null && selectedPartition.deviceName != null && !"".equals(selectedPartition.deviceName)) {
			return getFileSystemImageMD5(selectedPartition.deviceName, md5FileName);
		} else {
			return false;
		}
	}
	
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

	public FileEntry[] getRootFileEntries() {
		FileEntry root = selectedDevice.getIDevice().getFileListingService().getRoot();
		return getDirectoryEntries(root);
	}

	public FileEntry[] getDirectoryEntries(FileEntry fileEntry) {		
		FileEntry[] files = selectedDevice.getIDevice().getFileListingService().getChildren(fileEntry, true, null);
		return files;
	}

	public void executeShellCommand(String command, IShellOutputReceiver outputReceiver) {
		try {
			selectedDevice.executeShellCommand(command, outputReceiver);
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void rebootIntoRecovery() {
		try {
			selectedDevice.rebootIntoRecovery();
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void rebootIntoBootloader() {
		try {
			selectedDevice.rebootIntoBootloader();
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void reboot() {
		try {
			selectedDevice.reboot();
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
