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

import java.util.List;

import de.anddisa.adb.device.ITestDevice.PartitionInfo;

public class BackupProperties {

	private String baseDirectory;
	private boolean rootShell;
	private boolean busyboxInstalled;
	private List<PartitionInfo> partitionInfo;
		
	public BackupProperties() {
		super();
	}
	
	public String getBaseDirectory() {
		return baseDirectory;
	}
	public void setBaseDirectory(String baseDirectory) {
		this.baseDirectory = baseDirectory;
	}
	public boolean isRootShell() {
		return rootShell;
	}
	public void setRootShell(boolean rootShell) {
		this.rootShell = rootShell;
	}
	public boolean isBusyboxInstalled() {
		return busyboxInstalled;
	}
	public void setBusyboxInstalled(boolean busyboxInstalled) {
		this.busyboxInstalled = busyboxInstalled;
	}
	public List<PartitionInfo> getPartitionInfo() {
		return partitionInfo;
	}
	public void setPartitionInfo(List<PartitionInfo> partitionInfo) {
		this.partitionInfo = partitionInfo;
	}	
}
