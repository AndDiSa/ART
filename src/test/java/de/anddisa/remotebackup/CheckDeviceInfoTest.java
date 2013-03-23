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

import java.io.PrintWriter;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.anddisa.adb.device.DeviceManager;
import de.anddisa.adb.device.DeviceNotAvailableException;
import de.anddisa.adb.device.IDeviceManager;
import de.anddisa.adb.device.ITestDevice;
import de.anddisa.adb.device.ITestDevice.MountPointInfo;
import de.anddisa.adb.device.ITestDevice.PartitionInfo;
import de.anddisa.adb.device.TestDeviceState;
import de.anddisa.adb.util.CommandResult;

public class CheckDeviceInfoTest {
	
	private String getRootExecutableCommand(String command, boolean suWrapperNeeded) {
		if (suWrapperNeeded) {
			return "su -c \"" + command + "\"";
		}
		return command;
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
				System.out.println("Product type:" + allocateDevice.getFastbootProductType());
				System.out.println("Product variant:" + allocateDevice.getFastbootProductVariant());
				CommandResult commandResult = allocateDevice.executeFastbootCommand("getvar", "all");
				System.out.println("result.status:" + commandResult.getStatus());
				System.out.println("result.stderr:" +commandResult.getStderr());
				System.out.println("result.stdout:" +commandResult.getStdout());
			} else {
				System.out.println("Product type:" + allocateDevice.getProductType());
				System.out.println("Product variant:" + allocateDevice.getProductVariant());
				System.out.println("Build id:" + allocateDevice.getOptions());
				System.out.println("/proc/mtd:" + allocateDevice.executeShellCommand("cat /proc/mtd"));
				System.out.println("/proc/emmc:" + allocateDevice.executeShellCommand("cat /proc/emmc"));
				System.out.println("/fstab" + allocateDevice.executeShellCommand("cat /fstab*"));
//				System.out.println("/proc/mtd:" + allocateDevice.executeShellCommand("su -c cat /proc/mtd"));
//				System.out.println("/proc/emmc:" + allocateDevice.executeShellCommand("su -c cat /proc/emmc"));
//				System.out.println("/fstab" + allocateDevice.executeShellCommand("su -c cat /fstab*"));
				List<MountPointInfo> mountPointInfo = allocateDevice.getMountPointInfo();
				for (MountPointInfo mpi : mountPointInfo) {
					System.out.println(mpi.mountpoint + " " + mpi.type + " " + mpi.filesystem + " " + mpi.options);
				}
				System.out.println("/proc/fstab:" + allocateDevice.executeShellCommand(getRootExecutableCommand("cat /proc/fstab | busybox egrep \"(mtd|mmc|bml)\"`\" != \"\"", !allocateDevice.isAdbRoot())));
				System.out.println("/etc/fstab:" + allocateDevice.executeShellCommand(getRootExecutableCommand("busybox cat /etc/fstab", !allocateDevice.isAdbRoot())));
				System.out.println("/proc/mtd:" + allocateDevice.executeShellCommand(getRootExecutableCommand("busybox cat /proc/mtd", !allocateDevice.isAdbRoot())));
				System.out.println("/proc/emmc:" + allocateDevice.executeShellCommand(getRootExecutableCommand("busybox cat /proc/emmc", !allocateDevice.isAdbRoot())));
				System.out.println("/proc/partitions:" + allocateDevice.executeShellCommand(getRootExecutableCommand("busybox cat /proc/partitions", !allocateDevice.isAdbRoot())));
				System.out.println("/proc/cpuinfo:" + allocateDevice.executeShellCommand(getRootExecutableCommand("busybox cat /proc/cpuinfo", !allocateDevice.isAdbRoot())));
				PartitionInfo systemPartition = allocateDevice.getSystemPartition();
				PartitionInfo dataPartition = allocateDevice.getDataPartition();
				PartitionInfo cachePartition = allocateDevice.getCachePartition();
				PartitionInfo miscPartition = allocateDevice.getMiscPartition();
				PartitionInfo bootPartition = allocateDevice.getBootPartition();
				PartitionInfo recoveryPartition = allocateDevice.getRecoveryPartition();
				System.out.println("/proc/cpuinfo:" + allocateDevice.executeShellCommand(getRootExecutableCommand("busybox cat /proc/cpuinfo", !allocateDevice.isAdbRoot())));
			}
		} catch (DeviceNotAvailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail("No exception expected");
		}
	}
}
