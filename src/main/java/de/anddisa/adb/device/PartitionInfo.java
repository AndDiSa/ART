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
package de.anddisa.adb.device;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.XStream;

public class PartitionInfo  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1776940900715851962L;

	public enum DeviceType {
		Unknown,
		MTD,
		MMC
	}
    public String type;
    public Integer startBlock;
    public Integer blockCount;
    public String partitionName;
    public String flashFileName;
    public String deviceName;
    public String mountPoint;

    /** Simple constructor */
    public PartitionInfo() {}

    public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getStartBlock() {
		return startBlock;
	}

	public void setStartBlock(Integer startBlock) {
		this.startBlock = startBlock;
	}

	public Integer getBlockCount() {
		return blockCount;
	}

	public void setBlockCount(Integer blockCount) {
		this.blockCount = blockCount;
	}

	public String getPartitionName() {
		return partitionName;
	}

	public void setPartitionName(String partitionName) {
		this.partitionName = partitionName;
	}

	public String getFlashFileName() {
		return flashFileName;
	}

	public void setFlashFileName(String flashFileName) {
		this.flashFileName = flashFileName;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getMountPoint() {
		return mountPoint;
	}

	public void setMountPoint(String mountPoint) {
		this.mountPoint = mountPoint;
	}

	/**
     * Convenience constructor to set all members
     */
    public PartitionInfo(String type, Integer startBlock, Integer blockCount, String partitionName, String flashFileName, String deviceName, String mountPoint) {
        this.type = type;
        this.startBlock = startBlock;
        this.blockCount = blockCount;
        this.partitionName = partitionName;
        this.flashFileName = flashFileName;
        this.deviceName = deviceName;
        this.mountPoint = mountPoint;
    }
   
    public static List<PartitionInfo> getPartitionInfo(String device) {
    	return deviceMap.get(device);
    }
    
    @Override
    public String toString() {
        return String.format("%s %d %d %s %s %s %s", this.type, this.startBlock, this.blockCount, this.partitionName, this.flashFileName, this.deviceName, this.mountPoint);
    }
    
	public static Map<String, List<PartitionInfo>> deviceMap = new HashMap<String, List<PartitionInfo>>();
	
	static {
		List<PartitionInfo> trout = new ArrayList<PartitionInfo>();
		trout.add(new PartitionInfo("yaffs2", 0, 0, "misc", "misc.img", "/dev/mtd/mtd0ro", null));
		trout.add(new PartitionInfo("yaffs2", 0, 0, "recovery", "recovery.img", "/dev/mtd/mtd1ro", null));
		trout.add(new PartitionInfo("yaffs2", 0, 0, "boot", "boot.img", "/dev/mtd/mtd2ro", null));
		trout.add(new PartitionInfo("yaffs2", 0, 0, "system", "system.img", "/dev/mtd/mtd3ro", "/system"));
		trout.add(new PartitionInfo("yaffs2", 0, 0, "cache", "cache.img", "/dev/mtd/mtd4ro", "/cache"));
		trout.add(new PartitionInfo("yaffs2", 0, 0, "userdata", "userdata.img", "/dev/mtd/mtd5ro", "/data"));
		deviceMap.put("trout", trout);

		List<PartitionInfo> grouper = new ArrayList<PartitionInfo>();
		grouper.add(new PartitionInfo("ext4", 0, 12288, "SOS", "recovery.img", "/dev/block/mmcblk0p1", null));
		grouper.add(new PartitionInfo("ext4", 0, 8192, "LNX", "boot.img", "/dev/block/mmcblk0p2", null));
		grouper.add(new PartitionInfo("ext4", 0, 665600, "APP", "system.img", "/dev/block/mmcblk0p3", "/system"));
		grouper.add(new PartitionInfo("ext4", 0, 453632, "CAC", "cache.img", "/dev/block/mmcblk0p4", "/cache"));
		grouper.add(new PartitionInfo("ext4", 0, 512, "MSC", "misc.img", "/dev/block/mmcblk0p5", null));
		grouper.add(new PartitionInfo("ext4", 0, 10240, "USP", "bootloader.img", "/dev/block/mmcblk0p6", null));
		grouper.add(new PartitionInfo("ext4", 0, 5120, "PER", "", "/dev/block/mmcblk0p7", null));
		grouper.add(new PartitionInfo("ext4", 0, 512, "MDA", "", "/dev/block/mmcblk0p8", null));
		grouper.add(new PartitionInfo("ext4", 0, 0, "UDA", "userdata.img", "/dev/block/mmcblk0p9", "/data"));
		deviceMap.put("grouper", grouper);

		/*
		 * see http://forum.xda-developers.com/showthread.php?t=1104139
		 */
		List<PartitionInfo> i9100 = new ArrayList<PartitionInfo>();
		i9100.add(new PartitionInfo("ext4", 0, 0, "EFS", "efs.img", "/dev/block/mmcblk0p1", "/efs"));
		i9100.add(new PartitionInfo("ext4", 0, 0, "SBL1", "Sbl.bin", "/dev/block/mmcblk0p2", null));
		i9100.add(new PartitionInfo("ext4", 0, 0, "SBL2", "", "/dev/block/mmcblk0p3", null)); //TODO: rename flash filename 
		i9100.add(new PartitionInfo("j4fs", 0, 0, "PARAM", "param.lfs", "/dev/block/mmcblk0p4", null));
   		i9100.add(new PartitionInfo("ext4", 0, 0, "KERNEL", "zImage", "/dev/block/mmcblk0p5", null));
   	 	i9100.add(new PartitionInfo("ext4", 0, 0, "RECOVERY", "", "/dev/block/mmcblk0p6", null)); //TODO: rename flash filename 
		i9100.add(new PartitionInfo("ext4", 0, 0, "CACHE", "cache.img", "/dev/block/mmcblk0p7", "/cache"));
		i9100.add(new PartitionInfo("ext4", 0, 0, "MODEM", "modem.bin", "/dev/block/mmcblk0p8", null));
		i9100.add(new PartitionInfo("ext4", 0, 0, "FACTORYFS", "factoryfs.img", "/dev/block/mmcblk0p9", "/system"));
		i9100.add(new PartitionInfo("ext4", 0, 0, "DATAFS", "data.img", "/dev/block/mmcblk0p10", "/data"));
		i9100.add(new PartitionInfo("ext4", 0, 0, "UMS", "", "/dev/block/mmcblk0p11", "/sdcard")); //TODO: rename flash filename 
		i9100.add(new PartitionInfo("ext4", 0, 0, "HIDDEN", "hidden.img", "/dev/block/mmcblk0p12", "/preload"));
		deviceMap.put("SMDK4210", i9100);
	}
	
	public static void init(final String configFile) throws Exception {
		XStream xstream = new XStream();
		xstream.alias("PartitionInfo", PartitionInfo.class);
		xstream.useAttributeFor("type", String.class);
		xstream.useAttributeFor("startBlock", Integer.class);
		xstream.useAttributeFor("blockCount", Integer.class);
		xstream.useAttributeFor("partitionName", String.class);
		xstream.useAttributeFor("flashFileName", String.class);
		xstream.useAttributeFor("deviceName", String.class);
		xstream.useAttributeFor("mountPoint", String.class);

		deviceMap = (Map<String, List<PartitionInfo>>) xstream.fromXML(new FileInputStream(configFile));
	}

	public static void save(final String configFile) throws Exception {
		XStream xstream = new XStream();
		xstream.alias("PartitionInfo", PartitionInfo.class);
		xstream.useAttributeFor("type", String.class);
		xstream.useAttributeFor("startBlock", Integer.class);
		xstream.useAttributeFor("blockCount", Integer.class);
		xstream.useAttributeFor("partitionName", String.class);
		xstream.useAttributeFor("flashFileName", String.class);
		xstream.useAttributeFor("deviceName", String.class);
		xstream.useAttributeFor("mountPoint", String.class);
		xstream.toXML(deviceMap, new FileOutputStream(configFile));
	}
}