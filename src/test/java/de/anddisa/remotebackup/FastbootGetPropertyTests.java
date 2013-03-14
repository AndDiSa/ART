package de.anddisa.remotebackup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;

import de.anddisa.adb.device.DeviceManager;
import de.anddisa.adb.device.DeviceNotAvailableException;
import de.anddisa.adb.device.IDeviceManager;
import de.anddisa.adb.device.ITestDevice;
import de.anddisa.adb.device.TestDeviceState;
import de.anddisa.adb.util.CommandResult;

public class FastbootGetPropertyTests {
	 
	public class MyReceiver implements IShellOutputReceiver {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		public void addOutput(byte[] data, int offset, int length) {
			bos.write(data, offset, length);
		}

		public void flush() {
			try {
				bos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public boolean isCancelled() {
			return false;
		}
		
		public String getData() {
			try {
				return bos.toString("UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	}
	
	
	@Test
	public void test() {
		IDeviceManager deviceManager = DeviceManager.getInstance();
		deviceManager.init("/export/toolpool/adt-bundle-linux-x86/sdk/platform-tools/adb");
		/*
		DeviceSelectionOptions deviceSelection = new DeviceSelectionOptions();
		deviceSelection.addSerial("HT852KV37475");
		deviceSelection.addProductType("recovery");
		deviceSelection.addProductType("device");
		deviceManager.init(deviceSelection);
		*/
		ITestDevice allocateDevice = deviceManager.allocateDevice();
		deviceManager.displayDevicesInfo(new PrintWriter(System.out));
		try {
			TestDeviceState deviceState = allocateDevice.getDeviceState();
			if (deviceState.compareTo(TestDeviceState.FASTBOOT) == 0) {
				System.out.println("Bootloader version:" + allocateDevice.getBootloaderVersion());
				CommandResult commandResult = allocateDevice.executeFastbootCommand("getvar", "all");
				System.out.println("result.status:" + commandResult.getStatus());
				System.out.println("result.stderr:" +commandResult.getStderr());
				System.out.println("result.stdout:" +commandResult.getStdout());
			} else {
				IDevice iDevice = allocateDevice.getIDevice();
				Set<String> installedPackageNames = allocateDevice.getInstalledPackageNames();
				for (String string : installedPackageNames) {
					System.out.println(string);
				}
				System.out.println("shell command ls:" + allocateDevice.executeShellCommand("ls"));
				MyReceiver receiver = new MyReceiver();
				allocateDevice.executeShellCommand("cat /etc/vold.fstab", receiver);
				receiver.flush();
				System.out.println("shell command cat:" + receiver.getData());				
				System.out.println("adb command pull:" + allocateDevice.executeAdbCommand("pull", "/init.rc"));
			}
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail("No exception expected");
		}
	}
}
