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


/**
 * Thrown when a device is no longer available for testing.
 * e.g. the adb connection to the device has been lost, device has stopped responding to commands,
 * etc
 */
@SuppressWarnings("serial")
public class DeviceNotAvailableException extends Exception {
    /**
     * Creates a {@link DeviceNotAvailableException}.
     */
    public DeviceNotAvailableException() {
        super();
    }

    /**
     * Creates a {@link DeviceNotAvailableException}.
     *
     * @param msg a descriptive message.
     */
    public DeviceNotAvailableException(String msg) {
        super(msg);
    }

    /**
     * Creates a {@link DeviceNotAvailableException}.
     *
     * @param msg a descriptive message.
     * @param cause the root {@link Throwable} that caused the device to become unavailable.
     */
    public DeviceNotAvailableException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
