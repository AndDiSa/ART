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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GetSystemImageTestI9100 {

	static AdbWrapper adbWrapper = null;
	
	@BeforeClass
	public static void setUp() throws Exception {
		adbWrapper = new AdbWrapper("/export/toolpool/adt-bundle-linux-x86/sdk/platform-tools");
		adbWrapper.selectDevice(null);
	}

	@Test
	public void testGetSystemDump() throws Exception {
		/*
		Assert.assertTrue(adbWrapper.getPartitionAsImage("EFS", "/home/ds/nas-archive/systems/android/GT-I9100/backups/EFS.img"));
		Assert.assertTrue(adbWrapper.getPartitionMD5("EFS", "/home/ds/nas-archive/systems/android/GT-I9100/backups/EFS.img.md5"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("SBL1", "/home/ds/nas-archive/systems/android/GT-I9100/backups/SBL1.img"));
		Assert.assertTrue(adbWrapper.getPartitionMD5("SBL1", "/home/ds/nas-archive/systems/android/GT-I9100/backups/SBL1.img.md5"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("SBL2", "/home/ds/nas-archive/systems/android/GT-I9100/backups/SBL2.img"));
		Assert.assertTrue(adbWrapper.getPartitionMD5("SBL2", "/home/ds/nas-archive/systems/android/GT-I9100/backups/SBL2.img.md5"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("PARAM", "/home/ds/nas-archive/systems/android/GT-I9100/backups/PARAM.img"));
		Assert.assertTrue(adbWrapper.getPartitionMD5("PARAM", "/home/ds/nas-archive/systems/android/GT-I9100/backups/PARAM.img.md5"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("KERNEL", "/home/ds/nas-archive/systems/android/GT-I9100/backups/KERNEL.img"));
		Assert.assertTrue(adbWrapper.getPartitionMD5("KERNEL", "/home/ds/nas-archive/systems/android/GT-I9100/backups/KERNEL.img.md5"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("RECOVERY", "/home/ds/nas-archive/systems/android/GT-I9100/backups/RECOVERY.img"));
		Assert.assertTrue(adbWrapper.getPartitionMD5("RECOVERY", "/home/ds/nas-archive/systems/android/GT-I9100/backups/RECOVERY.img.md5"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("MODEM", "/home/ds/nas-archive/systems/android/GT-I9100/backups/MODEM.img"));
		Assert.assertTrue(adbWrapper.getPartitionMD5("MODEM", "/home/ds/nas-archive/systems/android/GT-I9100/backups/MODEM.img.md5"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("FACTORYFS", "/home/ds/nas-archive/systems/android/GT-I9100/backups/FACTORYFS.img"));
		Assert.assertTrue(adbWrapper.getPartitionMD5("FACTORYFS", "/home/ds/nas-archive/systems/android/GT-I9100/backups/FACTORYFS.img.md5"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("DATAFS", "/home/ds/nas-archive/systems/android/GT-I9100/backups/DATAFS.img"));
		Assert.assertTrue(adbWrapper.getPartitionMD5("DATAFS", "/home/ds/nas-archive/systems/android/GT-I9100/backups/DATAFS.img.md5"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("PRE", "/home/ds/nas-archive/systems/android/GT-I9100/backups/PRE.img"));
		Assert.assertTrue(adbWrapper.getPartitionMD5("PRE", "/home/ds/nas-archive/systems/android/GT-I9100/backups/PRE.img.md5"));
		*/
		Assert.assertTrue(AdbWrapper.compareMD5("/home/ds/nas-archive/systems/android/GT-I9100/backups/EFS.img", "/home/ds/nas-archive/systems/android/GT-I9100/backups/EFS.img.md5"));
		Assert.assertTrue(AdbWrapper.compareMD5("/home/ds/nas-archive/systems/android/GT-I9100/backups/SBL1.img", "/home/ds/nas-archive/systems/android/GT-I9100/backups/SBL1.img.md5"));
		Assert.assertTrue(AdbWrapper.compareMD5("/home/ds/nas-archive/systems/android/GT-I9100/backups/SBL2.img", "/home/ds/nas-archive/systems/android/GT-I9100/backups/SBL2.img.md5"));
		Assert.assertTrue(AdbWrapper.compareMD5("/home/ds/nas-archive/systems/android/GT-I9100/backups/PARAM.img", "/home/ds/nas-archive/systems/android/GT-I9100/backups/PARAM.img.md5"));
		Assert.assertTrue(AdbWrapper.compareMD5("/home/ds/nas-archive/systems/android/GT-I9100/backups/KERNEL.img", "/home/ds/nas-archive/systems/android/GT-I9100/backups/KERNEL.img.md5"));
		Assert.assertTrue(AdbWrapper.compareMD5("/home/ds/nas-archive/systems/android/GT-I9100/backups/RECOVERY.img", "/home/ds/nas-archive/systems/android/GT-I9100/backups/RECOVERY.img.md5"));
		Assert.assertTrue(AdbWrapper.compareMD5("/home/ds/nas-archive/systems/android/GT-I9100/backups/MODEM.img", "/home/ds/nas-archive/systems/android/GT-I9100/backups/MODEM.img.md5"));
		Assert.assertTrue(AdbWrapper.compareMD5("/home/ds/nas-archive/systems/android/GT-I9100/backups/FACTORYFS.img", "/home/ds/nas-archive/systems/android/GT-I9100/backups/FACTORYFS.img.md5"));
		Assert.assertTrue(AdbWrapper.compareMD5("/home/ds/nas-archive/systems/android/GT-I9100/backups/PRE.img", "/home/ds/nas-archive/systems/android/GT-I9100/backups/PRE.img.md5"));
		Assert.assertTrue(AdbWrapper.compareMD5("/home/ds/nas-archive/systems/android/GT-I9100/backups/DATAFS.img", "/home/ds/nas-archive/systems/android/GT-I9100/backups/DATAFS.img.md5"));
	}
}
