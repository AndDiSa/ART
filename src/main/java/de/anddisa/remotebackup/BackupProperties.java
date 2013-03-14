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
