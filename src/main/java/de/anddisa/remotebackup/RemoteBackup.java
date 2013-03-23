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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.anddisa.adb.device.DeviceNotAvailableException;
import de.anddisa.adb.device.ITestDevice;
import de.anddisa.adb.device.ITestDevice.PartitionInfo;

public class RemoteBackup {

    private static Options createCommandLineOptions() {
    	
        final Options options = new Options();
        OptionGroup commands = new OptionGroup();
        commands.addOption(OptionBuilder
				.withLongOpt("backup")
				.withDescription("backup partion(s)")
				.create("backup"));
		commands.addOption(OptionBuilder
				.withLongOpt("restore")
				.withDescription("restore partition(s)")
				.create("restore"));
		commands.addOption(OptionBuilder
				.withLongOpt("devices")
				.withDescription("list available devices")
				.create("devices"));
		commands.addOption(OptionBuilder
				.withLongOpt("info")
				.withDescription("info for devices")
				.create("info"));
		commands.addOption(OptionBuilder
				.withLongOpt("help")
				.withDescription("print help")
				.create("help"));
        options.addOptionGroup(commands);
        OptionGroup mode = new OptionGroup();
        mode.addOption(OptionBuilder
				.withLongOpt("img")
				.withDescription("image mode")
				.create("img"));
		mode.addOption(OptionBuilder
				.withLongOpt("tar")
				.withDescription("tar mode")
				.create("tar"));
        options.addOptionGroup(mode);
        OptionGroup location = new OptionGroup();
        location.addOption(OptionBuilder
				.withLongOpt("d")
				.withDescription("directory")
				.hasArg()
				.create("d"));
        location.addOption(OptionBuilder
				.withLongOpt("f")
				.withDescription("file")
				.hasArg()
				.create("f"));
        options.addOptionGroup(location);
        options.addOption(OptionBuilder
        		.withLongOpt("serial")
        		.withDescription("serial number")
        		.isRequired(false)
        		.hasArg()
        		.create("serial"));
        options.addOption(OptionBuilder
        		.withLongOpt("t")
        		.withDescription("timestamp")
        		.isRequired(false)
        		.hasArg()
        		.create("t"));
        options.addOption(OptionBuilder
        		.withLongOpt("tooldir")
        		.withDescription("tooldir path")
        		.isRequired(false)
        		.hasArg()
        		.create("tooldir"));
        return options;
    }

    private static String outputCommandLineHelp(final Options options) {
        final HelpFormatter formater = new HelpFormatter();
        formater.printHelp("Android Remote Tools:", options);
        return "";
    }

    private static String processCommandline(final CommandLine cl, final Options options) throws IllegalArgumentException, ParseException {
    	String adb = cl.getOptionValue("adb", null);
		AdbWrapper adbWrapper = new AdbWrapper(adb);
		String serial = cl.getOptionValue("serial", null);
		adbWrapper.selectDevice(serial);
        if ((null != cl) && cl.hasOption("devices")) {
            // do something with devices
        	return doDevices(adbWrapper, cl);
        }
       if ((null != cl) && cl.hasOption("backup")) {
        	return doBackup(adbWrapper, cl);
        }
        if ((null != cl) && cl.hasOption("restore")) {
        	return doRestore(adbWrapper, cl);
        }
        if ((null != cl) && cl.hasOption("info")) {
            // do something with devices
        	return doInfo(adbWrapper, cl);
        }
        return outputCommandLineHelp(options);
    }

	private static String doDevices(AdbWrapper adbWrapper, CommandLine cl) {
		StringBuffer sb = new StringBuffer();
		sb.append("connected devices:");
		sb.append('\n');
		Collection<String> devices = adbWrapper.getDevices();
		for (String device : devices) {
			sb.append(device);
			sb.append('\n');
		}
		return sb.toString();
	}

	private static String doInfo(AdbWrapper adbWrapper, CommandLine cl) {
		StringBuffer sb = new StringBuffer();
		sb.append("Partitions:");
		sb.append('\n');
		sb.append("Type StartBlock BlockCount PartitionName FlashFileName DeviceName mountPoint");
		sb.append('\n');
		sb.append("----------------------------------------------------------------------------");
		sb.append('\n');
		try {
			ITestDevice currentDevice = adbWrapper.getCurrentDevice();
			if (currentDevice != null) {
				List<PartitionInfo> partitionInfos = currentDevice.getPartitionInfo();
				for (PartitionInfo partitionInfo : partitionInfos) {
					sb.append(partitionInfo.toString());
					sb.append('\n');
				}
			} else {
				sb.append("error: device not available ...");
				sb.append('\n');
				
			}
		} catch (DeviceNotAvailableException e) {
			sb.append("error: device not available ...");
			sb.append('\n');
		}
		return sb.toString();
	}

	private static String doRestore(AdbWrapper adbWrapper, CommandLine cl) {
		// TODO Auto-generated method stub
		return null;
	}

	private static String doBackup(AdbWrapper adbWrapper, CommandLine cl) throws ParseException {
		String resultString = "";
		String backupMode = null;
		if (!cl.hasOption("img")) {
			if (cl.hasOption("tar")) {
				backupMode = "tar";
			}
		} else {
			backupMode = "img";
		}
		if (backupMode == null) {
			throw new ParseException("either img or tar must be set");
		}
		String directory = cl.getOptionValue("d", System.getProperty("user.dir"));
		if ("img".equals(backupMode)) {
			String[] partitions = cl.getArgs();
			boolean result = true;
			for (String partition : partitions) {
				PartitionInfo partitionInfo = adbWrapper.getCurrentDevice().getPartition(partition);
				String flashFileName = partitionInfo.flashFileName;
				if (flashFileName == null || "".equals(flashFileName)) {
					flashFileName = partitionInfo.partitionName + ".img";
				}
				flashFileName = directory + "/" + flashFileName;
				result |= adbWrapper.getPartitionAsImage(partition, flashFileName);
				result |= adbWrapper.getPartitionMD5(partition, flashFileName + ".md5");
				try {
					boolean compareMD5 = AdbWrapper.compareMD5(flashFileName, flashFileName + ".md5");
					if (!compareMD5) {
						resultString += flashFileName + "verification failed\n";
					}
				} catch (NoSuchAlgorithmException e) {
					result = false;
					break;
				} catch (IOException e) {
					result = false;
					break;
				}
			}
		} else {			
		}
		return resultString;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {	
		handleCommandLine(args);
	}

	public static void handleCommandLine(String[] args) {
		CommandLineParser parser = new GnuParser();
		Options commandLineOptions = createCommandLineOptions();
		try {
			CommandLine cmd = parser.parse(commandLineOptions, args);
			System.out.println(processCommandline(cmd, commandLineOptions));
		} catch (ParseException e) {
            outputCommandLineHelp(commandLineOptions);
		}
	}
}
