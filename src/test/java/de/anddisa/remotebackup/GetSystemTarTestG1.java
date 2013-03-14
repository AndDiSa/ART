package de.anddisa.remotebackup;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GetSystemTarTestG1 {

	static AdbWrapper adbWrapper = null;
	
	@BeforeClass
	public static void setUp() throws Exception {
		adbWrapper = new AdbWrapper("/export/toolpool/adt-bundle-linux-x86/sdk/platform-tools");
		adbWrapper.selectDevice(null);
	}

	@Test
	public void testGetSystemTar() {
		Assert.assertTrue(adbWrapper.getSytemPartitionAsTar("/tmp"));
	}

	@Test
	public void testGetDataTar() {
		Assert.assertTrue(adbWrapper.getDataPartitionAsTar("/tmp"));
	}

	@Test
	public void testGetExtTar() {
		Assert.assertTrue(adbWrapper.getExtPartitionAsTar("/tmp"));
	}
}
