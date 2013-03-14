/*
 * Copyright (C) 2011 The Android Open Source Project
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
 */

package de.anddisa.adb.device;

import de.anddisa.adb.device.WifiHelper.WifiState;

/**
 * Helper interface for manipulating wifi services on device.
 */
interface IWifiHelper {

    /**
     * Enables wifi state on device.
     *
     * @throws DeviceNotAvailableException
     */
    void enableWifi() throws DeviceNotAvailableException;

    /**
     * Disables wifi state on device.
     *
     * @throws DeviceNotAvailableException
     */
    void disableWifi() throws DeviceNotAvailableException;

    /**
     * Waits until one of the expected wifi states occurs.
     *
     * @param expectedStates one or more wifi states to expect
     * @param timeout max time in ms to wait
     * @return <code>true</code> if the one of the expected states occurred. <code>false</code> if
     *         none of the states occurred before timeout is reached
     * @throws DeviceNotAvailableException
     */
    boolean waitForWifiState(WifiState... expectedStates) throws DeviceNotAvailableException;

    /**
     * Adds the open security network identified by ssid.
     * <p/>
     * To connect to any wifi network, a network profile must be created in wpa_supplicant
     * configuration first. This will call wpa_cli to add the open security network identified by
     * ssid.
     *
     * @param ssid the ssid of network to add.
     * @return <code>true</code> if network was added successfully, <code>false</code> otherwise.
     * @throws DeviceNotAvailableException
     */
    boolean addOpenNetwork(String ssid) throws DeviceNotAvailableException;

    /**
     * Adds the WPA-PSK security network identified by ssid.
     *
     * @param ssid the ssid of network to add.
     * @param psk the WPA-PSK passphrase to use
     * @return <code>true</code> if network was added successfully, <code>false</code> otherwise.
     * @throws DeviceNotAvailableException
     */
    boolean addWpaPskNetwork(String ssid, String psk) throws DeviceNotAvailableException;

    /**
     * Wait until an ip address is assigned to wifi adapter.
     *
     * @param timeout how long to wait
     * @return <code>true</code> if an ip address is assigned before timeout, <code>false</code>
     *         otherwise
     * @throws DeviceNotAvailableException
     */
    boolean waitForIp(long timeout) throws DeviceNotAvailableException;

    /**
     * Gets the IP address associated with the wifi interface
     *
     */
    String getIpAddress() throws DeviceNotAvailableException;

    /**
     * Removes all known networks.
     *
     * @throws DeviceNotAvailableException
     */
    void removeAllNetworks() throws DeviceNotAvailableException;

}
