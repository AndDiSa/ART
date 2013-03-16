package de.anddisa.remotebackup;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GetSystemTarTestNexus {

	static AdbWrapper adbWrapper = null;
	
	@BeforeClass
	public static void setUp() throws Exception {
		adbWrapper = new AdbWrapper("/export/toolpool/adt-bundle-linux-x86/sdk/platform-tools");
		adbWrapper.selectDevice(null);
	}

	@Test
	public void testGetSystemTar() throws Exception {
		Assert.assertTrue(adbWrapper.getSytemPartitionAsTar("/home/ds/temp"));
	}

	@Test
	public void testGetDataTar() throws Exception {
		Assert.assertTrue(adbWrapper.getDataPartitionAsTar("/home/ds/temp"));
	}
}
