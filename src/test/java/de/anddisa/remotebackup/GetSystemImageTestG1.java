package de.anddisa.remotebackup;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GetSystemImageTestG1 {

	static AdbWrapper adbWrapper = null;
	
	@BeforeClass
	public static void setUp() throws Exception {
		adbWrapper = new AdbWrapper("/export/toolpool/adt-bundle-linux-x86/sdk/platform-tools");
		adbWrapper.selectDevice(null);
	}

	@Test
	public void testGetSystemImage() {
		Assert.assertTrue(adbWrapper.getSystemPartitionAsImage("/tmp/system.img"));
	}

	@Test
	public void testGetDataImage() {
		Assert.assertTrue(adbWrapper.getDataPartitionAsImage("/tmp/data.img"));
	}

	@Test
	public void testGetCacheImage() {
		Assert.assertTrue(adbWrapper.getCachePartitionAsImage("/tmp/cache.img"));
	}

	@Test
	public void testGetRecoveryImage() {
		Assert.assertTrue(adbWrapper.getRecoveryPartitionAsImage("/tmp/recovery.img"));
	}

	@Test
	public void testGetBootImage() {
		Assert.assertTrue(adbWrapper.getBootPartitionAsImage("/tmp/boot.img"));
	}

	//@Test
	public void testGetMiscImage() {
		Assert.assertTrue(adbWrapper.getMiscPartitionAsImage("/tmp/misc.img"));
	}
}
