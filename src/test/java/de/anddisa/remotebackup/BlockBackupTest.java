/*
 * (C) 2013 AndDiSa
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Test;

import com.android.ddmlib.IShellOutputReceiver;

import de.anddisa.adb.device.DeviceManager;
import de.anddisa.adb.device.DeviceNotAvailableException;
import de.anddisa.adb.device.IDeviceManager;
import de.anddisa.adb.device.ITestDevice;
import de.anddisa.adb.device.TestDeviceState;
import de.anddisa.adb.util.CommandResult;
import de.anddisa.adb.util.RunUtil;

public class BlockBackupTest {
	
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
		
		public int size() {
			return bos.size();
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
				System.out.println(allocateDevice.executeAdbCommand("forward", "tcp:5555", "tcp:5555"));
				System.out.println(allocateDevice.executeShellCommand("su -c /system/bin/rm -f /cache/myfifo"));
				System.out.println(allocateDevice.executeShellCommand("su -c /system/xbin/busybox mkfifo /cache/myfifo"));
				Process cmd1;
				Process cmd2;
				try {
					cmd1 = RunUtil.getDefault().runCmdInBackground("adb", "shell", "su -c \"/system/xbin/stty raw; /system/bin/dd if=/dev/block/platform/sdhci-tegra.3/by-name/SOS of=/cache/myfifo bs=4096\"");
					cmd2 = RunUtil.getDefault().runCmdInBackground("adb", "shell", "su -c \"/system/xbin/stty raw; /system/bin/cat /cache/myfifo\"");
					InputStream is = cmd2.getInputStream();
					int r = 0;
					int block = 0;
					byte[] buffer = new byte[4096];
					int size = 0;
					
					while ((r = is.read(buffer, 0,  buffer.length)) > 0) {
						System.out.println("read new block:" + ++block + " - bytes in block:" + r);
//						System.out.println(new String(buffer));
						size += r;
					}
					System.out.println("size: " + size);
					is.close();
					cmd1.exitValue();
					cmd1.destroy();
					cmd2.exitValue();
					cmd2.destroy();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//				System.out.println(allocateDevice.executeShellCommand("su -c dd if=/dev/block/platform/sdhci-tegra.3/by-name/CAC of=/dev/null"));
//				allocateDevice.executeAdbCommand("pull", "/init.rc");
			}
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail("No exception expected");
		}
	}
}
