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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.android.ddmlib.FileListingService.FileEntry;

import de.anddisa.adb.device.CollectingByteOutputReceiver;

public class AdbWrapperTests {

	static AdbWrapper adbWrapper = null;
	
	@BeforeClass
	public static void setUp() throws Exception {
		adbWrapper = new AdbWrapper("/export/toolpool/adt-bundle-linux-x86/sdk/platform-tools");
		adbWrapper.selectDevice(null);
	}

	@Test
	public void testExecuteLsShellCommand() {
		CollectingByteOutputReceiver os = new CollectingByteOutputReceiver();
		adbWrapper.executeShellCommand("ls", os);
		os.flush();
		System.out.println(new String(os.getOutput()));
	}

	//@Test
	public void testCopyRootDirectoryEntries() {
		String to = "/tmp/nexus7/";
		CollectingByteOutputReceiver os = new CollectingByteOutputReceiver();		
		adbWrapper.executeShellCommand("su", os);
		os.flush();
		System.out.println(new String(os.getOutput()));		
		FileEntry[] rootDirectoryEntries = adbWrapper.getRootFileEntries();
		Assert.assertNotNull(rootDirectoryEntries);
		for (FileEntry fileEntry : rootDirectoryEntries) {
			System.out.println(fileEntry.getName());
			adbWrapper.pullFile(fileEntry.getFullPath(), to);
		}
	}

	//@Test
	public void testCopyRootDir() {
		String to = "/tmp/nexus7/";
		adbWrapper.pullFile("/cache", to);
	}
	
	@Test
	public void testExecuteCopyFile() {
		adbWrapper.pullFile("/etc/vold.fstab", "/tmp/vold.fstab");
		File f = new File("/tmp/vold.fstab");
		Assert.assertNotNull(f);
	}
	
	@Test
	public void testExecuteCatShellCommand() {
		CollectingByteOutputReceiver os = new CollectingByteOutputReceiver();
		adbWrapper.executeShellCommand("/system/xbin/stty raw; cat /etc/vold.fstab", os);		
		try {
			os.flush();
			ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
			InputStream is = this.getClass().getResourceAsStream("/vold.fstab");
			byte[] buffer = new byte[1024];
			int i = 0;
			while ((i = is.read(buffer)) >= 0) {
				bos2.write(buffer,  0,  i);
			}
			bos2.flush();
			
			byte[] byteArray1 = os.getOutput();
			byte[] byteArray2 = bos2.toByteArray();

			Assert.assertEquals(byteArray1.length, byteArray2.length);
			for (int j = 0; i < byteArray1.length; j++) {
				System.out.println("" + byteArray1[j] + " : " + byteArray2[j]);
				Assert.assertEquals(byteArray1[j], byteArray2[j]);
			}
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
	
	@Test
	public void testExecuteCatBinaryShellCommand() {
		CollectingByteOutputReceiver os = new CollectingByteOutputReceiver();
		adbWrapper.executeShellCommand("/system/xbin/stty raw; cat /vendor/lib/libwvm.so", os);

		try {
			os.flush();
			ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
			InputStream is = this.getClass().getResourceAsStream("/libwvm.so");
			byte[] buffer = new byte[1024];
			int i = 0;
			while ((i = is.read(buffer)) >= 0) {
				bos2.write(buffer,  0,  i);
			}
			bos2.flush();

			byte[] byteArray1 = os.getOutput();
			byte[] byteArray2 = bos2.toByteArray();

			Assert.assertEquals(byteArray1.length, byteArray2.length);
			for (int j = 0; i < byteArray1.length; j++) {
				Assert.assertEquals(byteArray1[j], byteArray2[j]);
			}
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}	

	//@Test
	public void testReboot() {
		adbWrapper.reboot();
	}

	//@Test
	public void testRebootIntoRecovery() {
		adbWrapper.rebootIntoRecovery();
	}

	//@Test
	public void testRebootIntoBootloader() {
		adbWrapper.rebootIntoBootloader();
	}
}
