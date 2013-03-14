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
	public void testGetSystemDump() {
		Assert.assertTrue(adbWrapper.getPartitionAsImage("EFS", "/home/ds/nas-archive/systems/android/GT-I9100/backups/EFS.img"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("SBL1", "/home/ds/nas-archive/systems/android/GT-I9100/backups/SBL1.img"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("SBL2", "/home/ds/nas-archive/systems/android/GT-I9100/backups/SBL2.img"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("PARAM", "/home/ds/nas-archive/systems/android/GT-I9100/backups/PARAM.img"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("KERNEL", "/home/ds/nas-archive/systems/android/GT-I9100/backups/KERNEL.img"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("RECOVERY", "/home/ds/nas-archive/systems/android/GT-I9100/backups/RECOVERY.img"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("MODEM", "/home/ds/nas-archive/systems/android/GT-I9100/backups/MODEM.img"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("FACTORYFS", "/home/ds/nas-archive/systems/android/GT-I9100/backups/FACTORYFS.img"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("DATAFS", "/home/ds/nas-archive/systems/android/GT-I9100/backups/DATAFS.img"));
		Assert.assertTrue(adbWrapper.getPartitionAsImage("PRE", "/home/ds/nas-archive/systems/android/GT-I9100/backups/PRE.img"));
	}
}
