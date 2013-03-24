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

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
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
import de.anddisa.adb.device.PartitionInfo;
import de.anddisa.adb.device.TestDeviceState;
import de.anddisa.adb.device.ITestDevice.MountPointInfo;
import de.anddisa.adb.util.CommandResult;

public class RemoteBackup {

	/**
	 * recognised options
	 * 
	 * @return {@link Options}
	 */
    private static Options createCommandLineOptions() {
    	
        final Options options = new Options();
        OptionGroup commands = new OptionGroup();
        commands.addOption(OptionBuilder
				.withDescription("backup partion(s)")
				.create("backup"));
		commands.addOption(OptionBuilder
				.withDescription("restore partition(s)")
				.create("restore"));
		commands.addOption(OptionBuilder
				.withDescription("list available devices")
				.create("devices"));
		commands.addOption(OptionBuilder
				.withDescription("dump device info")
				.create("info"));
		commands.addOption(OptionBuilder
				.withLongOpt("help")
				.withDescription("print help")
				.create("h"));
        options.addOptionGroup(commands);
        OptionGroup mode = new OptionGroup();
        mode.addOption(OptionBuilder
				.withLongOpt("image")
				.withDescription("use image mode")
				.create("i"));
		mode.addOption(OptionBuilder
				.withLongOpt("tar")
				.withDescription("use tar mode")
				.create("t"));
        options.addOptionGroup(mode);
        OptionGroup location = new OptionGroup();
        location.addOption(OptionBuilder
				.withLongOpt("baseDir")
				.withDescription("base directory to backup to / restore from")
				.hasArg()
				.create("bd"));
        location.addOption(OptionBuilder
				.withLongOpt("file")
				.withDescription("file to restore from")
				.hasArg()
				.create("f"));
        options.addOptionGroup(location);
        options.addOption(OptionBuilder
        		.withLongOpt("serial")
        		.withDescription("connect tot device with serial number")
        		.isRequired(false)
        		.hasArg()
        		.create("s"));
        options.addOption(OptionBuilder
        		.withLongOpt("timeStampFormat")
        		.withDescription("create timestamped sub directory in backup mode using format (e.g. 'YYYY-MM-DD-HH-mm'")
        		.isRequired(false)
        		.hasArg()
        		.create("tsf"));
        options.addOption(OptionBuilder
        		.withLongOpt("tooldir")
        		.withDescription("defined tooldir path")
        		.isRequired(false)
        		.hasArg()
        		.create("td"));
        options.addOption(OptionBuilder
        		.withLongOpt("partitionInfoFile")
        		.withDescription("partitionInfoFile to be used for initialization")
        		.isRequired(false)
        		.hasArg()
        		.create("pif"));
        return options;
    }

    /**
     * prints help message
     * 
     * @param message
     * @param options
     * @return 
     */
    private static String outputCommandLineHelp(final String message, final Options options) {
        final HelpFormatter formater = new HelpFormatter();
        formater.printHelp("art", "", options, message);
        return "";
    }

    /**
     * processes the command line input
     * 
     * @param cl
     * @param options
     * @return
     * 
     * @throws IllegalArgumentException
     * @throws ParseException
     * @throws ApplicationException
     */
    private static String processCommandline(final CommandLine cl, final Options options) throws IllegalArgumentException, ParseException, ApplicationException {
    	String adb = cl.getOptionValue("td", null);
		AdbWrapper adbWrapper = new AdbWrapper(adb);
		String serial = cl.getOptionValue("s", null);
		adbWrapper.selectDevice(serial);
		String pif = cl.getOptionValue("pif", null);
		if (pif != null) {
			try {
				PartitionInfo.init(pif);
			} catch (Exception e) {
				throw new ApplicationException(e.getMessage());
			}
		}
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
        return outputCommandLineHelp("", options);
    }

    /**
     * devices command
     * 
     * @param adbWrapper
     * @param cl
     * @return
     */
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

	/**
	 * info command
	 * 
	 * @param adbWrapper
	 * @param cl
	 * @return
	 */
	private static String doInfo(AdbWrapper adbWrapper, CommandLine cl) {
		StringBuffer sb = new StringBuffer();
		try {
			ITestDevice currentDevice = adbWrapper.getCurrentDevice();
			if (currentDevice != null) {
				sb.append("Partitions:");
				sb.append('\n');
				sb.append("Type StartBlock BlockCount PartitionName FlashFileName DeviceName mountPoint");
				sb.append('\n');
				sb.append("----------------------------------------------------------------------------");
				sb.append('\n');
				List<PartitionInfo> partitionInfos = currentDevice.getPartitionInfo();
				for (PartitionInfo partitionInfo : partitionInfos) {
					sb.append(partitionInfo.toString());
					sb.append('\n');
				}

				if (currentDevice.getDeviceState().compareTo(TestDeviceState.FASTBOOT) == 0) {
					System.out.println("Bootloader version:" + currentDevice.getBootloaderVersion());
					System.out.println("Product type:" + currentDevice.getFastbootProductType());
					System.out.println("Product variant:" + currentDevice.getFastbootProductVariant());
					CommandResult commandResult = currentDevice.executeFastbootCommand("getvar", "all");
					System.out.println("result.status:" + commandResult.getStatus());
					System.out.println("result.stderr:" +commandResult.getStderr());
					System.out.println("result.stdout:" +commandResult.getStdout());
				} else {
					System.out.println("Product type:" + currentDevice.getProductType());
					System.out.println("Product variant:" + currentDevice.getProductVariant());
					System.out.println("Build id:" + currentDevice.getOptions());
					List<MountPointInfo> mountPointInfo = currentDevice.getMountPointInfo();
					for (MountPointInfo mpi : mountPointInfo) {
						System.out.println(mpi.mountpoint + " " + mpi.type + " " + mpi.filesystem + " " + mpi.options);
					}
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

	/**
	 * restore command
	 * 
	 * @param adbWrapper
	 * @param cl
	 * @return
	 * @throws ApplicationException
	 */
	private static String doRestore(AdbWrapper adbWrapper, CommandLine cl)  throws ApplicationException {
		throw new ApplicationException("not implemented yet");
	}

	/**
	 * backup command
	 * 
	 * @param adbWrapper
	 * @param cl
	 * @return
	 * @throws ApplicationException
	 */
	private static String doBackup(AdbWrapper adbWrapper, CommandLine cl) throws ApplicationException {
		String resultString = "";
		String backupMode = null;
		if (!cl.hasOption("i")) {
			if (cl.hasOption("t")) {
				backupMode = "tar";
			}
		} else {
			backupMode = "img";
		}
		if (backupMode == null) {
			throw new ApplicationException("either -i or -t must be set");
		}
		String subDir = "";
		if (cl.hasOption("tsf")) {
			DateFormat df = new SimpleDateFormat(cl.getOptionValue("tsf"));
			subDir = df.format(new Date());
		}
		String directory = cl.getOptionValue("bd", System.getProperty("user.dir"));
		if ("img".equals(backupMode)) {
			String[] partitions = cl.getArgs();
			boolean result = true;
			for (String partition : partitions) {
				PartitionInfo partitionInfo = adbWrapper.getCurrentDevice().getPartition(partition);
				if (partitionInfo != null) {
					String flashFileName = partitionInfo.flashFileName;
					if (flashFileName == null || "".equals(flashFileName)) {
						flashFileName = partitionInfo.partitionName + ".img";
					}
					String flashDir = directory + ("".equals(subDir) ? "" : "/" + subDir );
					File f = new File(flashDir);
					if (!f.exists()) {
						if (!f.mkdirs()) {
							throw new ApplicationException("cannot create directory: " + flashDir);							
						}
					}
					flashFileName = flashDir + "/" + flashFileName;
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
				} else {
					throw new ApplicationException("unknown partition name for device: " + partition);
				}
			}
			if (!result) {
				resultString += "error!";
			}
		} else {
			String[] mountPoints = cl.getArgs();
			boolean result = true;
			for (String mountPoint : mountPoints) {
				String flashDir = directory + ("".equals(subDir) ? "" : "/" + subDir );
				File f = new File(flashDir);
				if (!f.exists()) {
					if (!f.mkdirs()) {
						throw new ApplicationException("cannot create directory: " + flashDir);							
					}
				}
				try {
					result = adbWrapper.getMountPointAsTar(mountPoint, flashDir);
				} catch (NoSuchAlgorithmException e) {
					result = false;
					break;
				} catch (IOException e) {
					result = false;
					break;
				}
			}
			if (!result) {
				resultString += "error!";
			}
		}
		return resultString;
	}

	/**
	 * interprets the command
	 * 
	 * @param args
	 */
	public static void handleCommandLine(String[] args) {
		CommandLineParser parser = new GnuParser();
		Options commandLineOptions = createCommandLineOptions();
		try {
			CommandLine cmd = parser.parse(commandLineOptions, args);
			System.out.println(processCommandline(cmd, commandLineOptions));
		} catch (ParseException e) {
            outputCommandLineHelp(e.getMessage(), commandLineOptions);
		} catch (ApplicationException e) {
	        outputCommandLineHelp(e.getMessage(), commandLineOptions);
		}
	}
	
	/**
	 * main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			handleCommandLine(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}
