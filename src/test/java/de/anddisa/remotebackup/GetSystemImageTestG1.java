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

public class GetSystemImageTestG1 {

	static AdbWrapper adbWrapper = null;
	
	@BeforeClass
	public static void setUp() throws Exception {
		adbWrapper = new AdbWrapper("/export/toolpool/adt-bundle-linux-x86/sdk/platform-tools");
		adbWrapper.selectDevice(null);
	}

	@Test
	public void testGetSystemImage() {
		Assert.assertTrue(adbWrapper.getPartitionAsImage("system", "/tmp/system.img"));
	}

	@Test
	public void testGetDataImage() {
		Assert.assertTrue(adbWrapper.getPartitionAsImage("data", "/tmp/data.img"));
	}

	@Test
	public void testGetCacheImage() {
		Assert.assertTrue(adbWrapper.getPartitionAsImage("cache", "/tmp/cache.img"));
	}

	@Test
	public void testGetRecoveryImage() {
		Assert.assertTrue(adbWrapper.getPartitionAsImage("recovery", "/tmp/recovery.img"));
	}

	@Test
	public void testGetBootImage() {
		Assert.assertTrue(adbWrapper.getPartitionAsImage("boot", "/tmp/boot.img"));
	}

	//@Test
	public void testGetMiscImage() {
		Assert.assertTrue(adbWrapper.getPartitionAsImage("misc", "/tmp/misc.img"));
	}
}
