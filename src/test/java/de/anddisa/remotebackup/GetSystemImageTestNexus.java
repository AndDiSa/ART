package de.anddisa.remotebackup;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GetSystemImageTestNexus {

	static AdbWrapper adbWrapper = null;
	
	@BeforeClass
	public static void setUp() throws Exception {
		adbWrapper = new AdbWrapper("/export/toolpool/adt-bundle-linux-x86/sdk/platform-tools");
		adbWrapper.selectDevice(null);
	}

	@Test
	public void testGetSystemImage() {
		Assert.assertTrue(adbWrapper.getSystemPartitionAsImage("/home/ds/nas-archive/systems/android/nexus7/backups/system.img"));
	}

	@Test
	public void testGetDataImage() {
		Assert.assertTrue(adbWrapper.getDataPartitionAsImage("/home/ds/nas-archive/systems/android/nexus7/backups/data.img"));
	}

	@Test
	public void testGetCacheImage() {
		Assert.assertTrue(adbWrapper.getCachePartitionAsImage("/home/ds/nas-archive/systems/android/nexus7/backups/cache.img"));
	}

	@Test
	public void testGetRecoveryImage() {
		Assert.assertTrue(adbWrapper.getRecoveryPartitionAsImage("/home/ds/nas-archive/systems/android/nexus7/backups/recovery.img"));
	}

	@Test
	public void testGetBootImage() {
		Assert.assertTrue(adbWrapper.getBootPartitionAsImage("/home/ds/nas-archive/systems/android/nexus7/backups/boot.img"));
	}

	@Test
	public void testGetMiscImage() {
		Assert.assertTrue(adbWrapper.getMiscPartitionAsImage("/home/ds/nas-archive/systems/android/nexus7/backups/misc.img"));
	}
}
