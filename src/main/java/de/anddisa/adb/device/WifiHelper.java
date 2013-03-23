/*
 * Copyright (C) 2010 The Android Open Source Project
 *
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
 * 
 * 
 * based on the android tradefederation project
 * modified by AndDiSa 
 */
package de.anddisa.adb.device;

import com.android.ddmlib.MultiLineReceiver;
import de.anddisa.adb.targetprep.TargetSetupError;
import de.anddisa.adb.util.FileUtil;
import de.anddisa.adb.util.IRunUtil;
import de.anddisa.adb.util.RunUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for manipulating wifi services on device.
 */
public class WifiHelper implements IWifiHelper {

    private static final String NULL_IP_ADDR = "0.0.0.0";
    private static final String INTERFACE_KEY = "interface";
    private static final String INSTRUMENTATION_CLASS = ".WifiUtil";
    private static final String INSTRUMENTATION_PKG = "com.android.tradefed.utils.wifi";
    static final String FULL_INSTRUMENTATION_NAME =
            String.format("%s/%s", INSTRUMENTATION_PKG, INSTRUMENTATION_CLASS);

    private static final String CHECK_INSTRUMENTATION_CMD =
            String.format("pm list instrumentation %s", INSTRUMENTATION_PKG);

    private static final String WIFIUTIL_APK_NAME = "WifiUtil.apk";

    enum WifiState {
        COMPLETED, SCANNING, DISCONNECTED;
    }

    /** token used to detect proper command response */
    static final String SUCCESS_MARKER = "tHiS-iS-sUcCeSs-MaRkEr";

    private static final String WPA_STATE = "wpa_state";

    /** the default time in ms to wait for a wifi state */
    private static final long DEFAULT_WIFI_STATE_TIMEOUT = 30*1000;

    private final ITestDevice mDevice;

    public WifiHelper(ITestDevice device) throws TargetSetupError, DeviceNotAvailableException {
        mDevice = device;
        ensureDeviceSetup();
    }

    /**
     * Get the {@link RunUtil} instance to use.
     * <p/>
     * Exposed for unit testing.
     */
    IRunUtil getRunUtil() {
        return RunUtil.getDefault();
    }

    void ensureDeviceSetup() throws TargetSetupError, DeviceNotAvailableException {
        final String inst = mDevice.executeShellCommand(CHECK_INSTRUMENTATION_CMD);
        if ((inst != null) && inst.contains(FULL_INSTRUMENTATION_NAME)) {
            // Good to go
            return;
        } else {
            // Attempt to install utility
            File apkTempFile = null;
            try {
                apkTempFile = FileUtil.createTempFile(WIFIUTIL_APK_NAME, ".apk");
                InputStream apkStream = getClass().getResourceAsStream(
                    String.format("/apks/wifiutil/%s", WIFIUTIL_APK_NAME));
                FileUtil.writeToFile(apkStream, apkTempFile);

                final String result = mDevice.installPackage(apkTempFile, false);
                if (result == null) {
                    // Installed successfully; good to go.
                    return;
                } else {
                    throw new TargetSetupError(String.format(
                            "Unable to install WifiUtil utility: %s", result));
                }
            } catch (IOException e) {
                throw new TargetSetupError(String.format(
                        "Failed to unpack WifiUtil utility: %s", e.getMessage()));
            } finally {
                FileUtil.deleteFile(apkTempFile);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableWifi() throws DeviceNotAvailableException {
        mDevice.executeShellCommand("svc wifi enable");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableWifi() throws DeviceNotAvailableException {
        mDevice.executeShellCommand("svc wifi disable");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean waitForWifiState(WifiState... expectedStates) throws DeviceNotAvailableException {
        return waitForWifiState(DEFAULT_WIFI_STATE_TIMEOUT, expectedStates);
    }

    /**
     * Waits the given time until one of the expected wifi states occurs.
     *
     * @param expectedStates one or more wifi states to expect
     * @param timeout max time in ms to wait
     * @return <code>true</code> if the one of the expected states occurred. <code>false</code> if
     *         none of the states occurred before timeout is reached
     * @throws DeviceNotAvailableException
     */
     boolean waitForWifiState(long timeout, WifiState... expectedStates)
            throws DeviceNotAvailableException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < (startTime + timeout)) {
            Map<String, String> statusMap = getWifiStatus();
            if (statusMap != null) {
                String state = statusMap.get(WPA_STATE);
                for (WifiState expectedState : expectedStates) {
                    if (expectedState.name().equals(state)) {
                        return true;
                    }
                }
            }
            getRunUtil().sleep(getPollTime());
        }
        return false;
    }

    /**
     * Gets the time to sleep between poll attempts
     */
    long getPollTime() {
        return 1*1000;
    }

    /**
     * Retrieves wifi status from device.
     * <p/>
     * This will call 'wpa_cli' status on device. Typical output looks like:
     *
     * <pre>
     * Using interface 'tiwlan0'
     * bssid=00:0b:86:c3:80:40
     * ssid=ssidname
     * id=0
     * pairwise_cipher=NONE
     * group_cipher=NONE
     * key_mgmt=NONE
     * wpa_state=COMPLETED
     * </pre>
     *
     * Each line will be converted to an entry in the resulting map, with an additional key
     * {@link #INTERFACE_KEY} indicating the interface name.
     *
     * @return a map containing wifi status variables. <code>null</code> if wpa_cli failed to
     *         connect to wpa_supplicant
     * @throws DeviceNotAvailableException
     */
    Map<String, String> getWifiStatus() throws DeviceNotAvailableException {
        Map<String, String> statusMap = new HashMap<String, String>();
        WpaCliOutput output = callWpaCli("status");
        if (!output.isSuccess()) {
            return null;
        }
        for (String line: output.mOutputLines) {
            String[] pair = line.split("=", 2);
            if (pair.length == 2) {
                statusMap.put(pair[0], pair[1]);
            }
        }
        if (!statusMap.containsKey(WPA_STATE)) {
            return null;
        }
        statusMap.put(INTERFACE_KEY, output.mWpaInterface);
        return statusMap;
    }

    /**
     * Remove the network identified by an integer network id.
     *
     * @param networkId the network id identifying its profile in wpa_supplicant configuration
     * @throws DeviceNotAvailableException
     */
    void removeNetwork(int networkId) throws DeviceNotAvailableException {
        runWifiUtil("removeNetwork", "id", Integer.toString(networkId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addOpenNetwork(String ssid) throws DeviceNotAvailableException {
        int id = asInt(runWifiUtil("addOpenNetwork", "ssid", ssid));
        if (id < 0) {
            return false;
        }
        if (!asBool(runWifiUtil("associateNetwork", "id", Integer.toString(id)))) {
            return false;
        }
        if (!asBool(runWifiUtil("saveConfiguration"))) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addWpaPskNetwork(String ssid, String psk) throws DeviceNotAvailableException {
        int id = asInt(runWifiUtil("addWpaPskNetwork", "ssid", ssid, "psk", psk));
        if (id < 0) {
            return false;
        }
        if (!asBool(runWifiUtil("associateNetwork", "id", Integer.toString(id)))) {
            return false;
        }
        if (!asBool(runWifiUtil("saveConfiguration"))) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean waitForIp(long timeout) throws DeviceNotAvailableException {
        if (!asBool(runWifiUtil("isWifiEnabled"))) {
            return false;
        }

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() < (startTime + timeout)) {
            final String ip = getIpAddress();
            if (!ip.isEmpty() && !NULL_IP_ADDR.equals(getIpAddress())) {
                return true;
            }
            getRunUtil().sleep(getPollTime());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpAddress() throws DeviceNotAvailableException {
        return runWifiUtil("getIpAddress");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllNetworks() throws DeviceNotAvailableException {
        runWifiUtil("removeAllNetworks");
    }

    /**
     * Run a WifiUtil command and return the result
     *
     * @param method the WifiUtil method to call
     * @param args a flat list of [arg-name, value] pairs to pass
     * @return The value of the result field in the output
     */
    private String runWifiUtil(String method, String... args) throws DeviceNotAvailableException {
        final String cmd = buildWifiUtilCmd(method, args);

        WifiUtilOutput parser = new WifiUtilOutput();
        mDevice.executeShellCommand(cmd, parser);
        return parser.mResult;
    }

    /**
     * Build and return a WifiUtil command for the specified method and args
     *
     * @param method the WifiUtil method to call
     * @param args a flat list of [arg-name, value] pairs to pass
     * @return the command to be executed on the device shell
     */
    static String buildWifiUtilCmd(String method, String... args) {
        Map<String, String> argMap = new HashMap<String, String>();
        argMap.put("method", method);
        if ((args.length & 0x1) == 0x1) {
            throw new IllegalArgumentException(
                    "args should have even length, consisting of key and value pairs");
        }
        for (int i = 0; i < args.length; i += 2) {
            argMap.put(args[i], args[i+1]);
        }
        return buildWifiUtilCmdFromMap(argMap);
    }

    /**
     * Build and return a WifiUtil command for the specified args
     *
     * @param args A Map of (arg-name, value) pairs to pass as "-e" arguments to the `am` command
     * @return the commadn to be executed on the device shell
     */
    static String buildWifiUtilCmdFromMap(Map<String, String> args) {
        StringBuilder sb = new StringBuilder("am instrument");

        for (Map.Entry<String, String> arg : args.entrySet()) {
            sb.append(" -e ");
            sb.append(arg.getKey());
            sb.append(" ");
            sb.append(quote(arg.getValue()));
        }

        sb.append(" -w ");
        sb.append(INSTRUMENTATION_PKG);
        sb.append("/");
        sb.append(INSTRUMENTATION_CLASS);

        return sb.toString();
    }

    /**
     * Helper function to convert a String to an Integer
     */
    private static int asInt(String str) {
        if (str == null) {
            return -1;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Helper function to convert a String to a boolean.  Maps "true" to true, and everything else
     * to false.
     */
    private static boolean asBool(String str) {
        return "true".equals(str);
    }

    /**
     * Helper function to wrap the specified String in double-quotes to prevent shell interpretation
     */
    private static String quote(String str) {
        return String.format("\"%s\"", str);
    }

    /**
     * Calls wpa_cli and does initial parsing.
     *
     * @param cmd the wpa_cli command to run
     * @return a WpaCliOutput object containing the result of the command. If an error is detected
     *         in wpa_cli output, e.g. failed to connect to wpa_supplicant, which is typically due
     *         to disabled wifi, <code>null</code> will be returned
     * @throws DeviceNotAvailableException
     */
    private WpaCliOutput callWpaCli(String cmd) throws DeviceNotAvailableException {
        String fullCmd = String.format("wpa_cli %s && echo && echo %s", cmd, SUCCESS_MARKER);
        WpaCliOutput output = new WpaCliOutput();
        mDevice.executeShellCommand(fullCmd, output);
        return output;
    }

    /**
     * Processes the output of a wpa_cli command.
     */
    private static class WpaCliOutput extends MultiLineReceiver {

        private boolean mDidCommandComplete = false;
        private boolean mIsCommandSuccess = true;

        /** The name of the interface resulting from a wpa cli command */
        String mWpaInterface = null;

        /** The output lines of the wpa cli command. */
        List<String> mOutputLines;

        WpaCliOutput() {
            mOutputLines = new ArrayList<String>();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void processNewLines(String[] lines) {
            // expect SUCCESS_MARKER to be present on last line for successful command
            if (!mDidCommandComplete) {
                mDidCommandComplete = lines[lines.length -1].equals(SUCCESS_MARKER);
            }
            Pattern interfacePattern = Pattern.compile("Using interface '(.*)'");

            for (String line : lines) {
                mOutputLines.add(line);
                if (line.contains("Failed to connect to wpa_supplicant")) {
                    mIsCommandSuccess = false;
                }
                Matcher interfaceMatcher = interfacePattern.matcher(line);
                if (interfaceMatcher.find()) {
                    mWpaInterface = interfaceMatcher.group(1);
                }
            }
        }

        public boolean isSuccess() {
            return mDidCommandComplete && mIsCommandSuccess;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCancelled() {
            return false;
        }
    }


    /**
     * Processes the output of a WifiUtil invocation
     */
    private static class WifiUtilOutput extends MultiLineReceiver {

        private boolean mDidCommandComplete = false;

        private static final String INST_SUCCESS_MARKER = "INSTRUMENTATION_CODE: -1";
        private static final Pattern RESULT_PAT =
                Pattern.compile("INSTRUMENTATION_RESULT: result=(.*)");

        String mResult = null;

        /** The output lines of the WifiUtil invocation. */
        List<String> mOutputLines;

        WifiUtilOutput() {
            mOutputLines = new ArrayList<String>();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void processNewLines(String[] lines) {
            // expect INST_SUCCESS_MARKER to be present on last line for successful command
            if (!mDidCommandComplete) {
                mDidCommandComplete = lines[lines.length - 1].equals(INST_SUCCESS_MARKER);
            }

            for (String line : lines) {
                mOutputLines.add(line);
                Matcher resultMatcher = RESULT_PAT.matcher(line);
                if (resultMatcher.matches()) {
                    mResult = resultMatcher.group(1);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}

