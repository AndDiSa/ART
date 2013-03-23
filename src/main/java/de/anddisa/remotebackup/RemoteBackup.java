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

import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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

    private static String processCommandline(final CommandLine cl, final Options options) throws IllegalArgumentException {
        if ((null != cl) && cl.hasOption("backup")) {
        	return doBackup(cl);
        }
        if ((null != cl) && cl.hasOption("restore")) {
        	return doRestore(cl);
        }
        if ((null != cl) && cl.hasOption("devices")) {
            // do something with devices
        	return doDevices(cl);
        }
        return outputCommandLineHelp(options);
    }

	private static String doDevices(CommandLine cl) {
		String adb = cl.getOptionValue("adb", null);
		AdbWrapper adbWrapper = new AdbWrapper(adb);
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

	private static String doRestore(CommandLine cl) {
		// TODO Auto-generated method stub
		return null;
	}

	private static String doBackup(CommandLine cl) {
		// TODO Auto-generated method stub
		return null;
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
