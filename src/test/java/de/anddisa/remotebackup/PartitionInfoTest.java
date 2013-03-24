package de.anddisa.remotebackup;

import org.junit.Before;
import org.junit.Test;

import de.anddisa.adb.device.PartitionInfo;

public class PartitionInfoTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() throws Exception {
		PartitionInfo.save("/tmp/partitionInfos.xml");
		PartitionInfo.init("/tmp/partitionInfos.xml");
//		PartitionInfo.save("/tmp/partitionInfos2.xml");		
	}
}
