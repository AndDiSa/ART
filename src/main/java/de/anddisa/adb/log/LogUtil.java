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

package de.anddisa.adb.log;

import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A logging utility class.  Useful for code that needs to override static methods from {@link Log}
 */
public class LogUtil {

    /**
     * Make uninstantiable
     */
    private LogUtil() {}

    /**
     * Sent when a log message needs to be printed.  This implementation prints the message to
     * stdout in all cases.
     *
     * @param logLevel The {@link LogLevel} enum representing the priority of the message.
     * @param tag The tag associated with the message.
     * @param message The message to display.
     */
    public static void printLog(LogLevel logLevel, String tag, String message) {
        System.out.print(LogUtil.getLogFormatString(logLevel, tag, message));
    }

    /**
     * Creates a format string that is similar to the "threadtime" log format on the device.  This
     * is specifically useful because it includes the day and month (to differentiate times for
     * long-running TF instances), and also uses 24-hour time to disambiguate morning from evening.
     * <p/>
     * {@see Log#getLogFormatString()}
     */
    public static String getLogFormatString(LogLevel logLevel, String tag, String message) {
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss");
        return String.format("%s %c/%s: %s\n", formatter.format(new Date()),
                logLevel.getPriorityLetter(), tag, message);
    }

    /**
     * A shim class for {@link Log} that automatically uses the simple classname of the caller as
     * the log tag
     */
    public static class CLog {
        /**
         * The shim version of {@link Log#v(String, String)}.
         *
         * @param message The {@code String} to log
         */
        public static void v(String message) {
            // frame 2: skip frames 0 (#getClassName) and 1 (this method)
            Log.v(getClassName(2), message);
        }

        /**
         * The shim version of {@link Log#v(String, String)}.  Also calls String.format for
         * convenience.
         *
         * @param format A format string for the message to log
         * @param args The format string arguments
         */
        public static void v(String format, Object... args) {
            // frame 2: skip frames 0 (#getClassName) and 1 (this method)
            Log.v(getClassName(2), String.format(format, args));
        }

        /**
         * The shim version of {@link Log#d(String, String)}.
         *
         * @param message The {@code String} to log
         */
        public static void d(String message) {
            // frame 2: skip frames 0 (#getClassName) and 1 (this method)
            Log.d(getClassName(2), message);
        }

        /**
         * The shim version of {@link Log#d(String, String)}.  Also calls String.format for
         * convenience.
         *
         * @param format A format string for the message to log
         * @param args The format string arguments
         */
        public static void d(String format, Object... args) {
            // frame 2: skip frames 0 (#getClassName) and 1 (this method)
            Log.d(getClassName(2), String.format(format, args));
        }

        /**
         * The shim version of {@link Log#i(String, String)}.
         *
         * @param message The {@code String} to log
         */
        public static void i(String message) {
            // frame 2: skip frames 0 (#getClassName) and 1 (this method)
            Log.i(getClassName(2), message);
        }

        /**
         * The shim version of {@link Log#i(String, String)}.  Also calls String.format for
         * convenience.
         *
         * @param format A format string for the message to log
         * @param args The format string arguments
         */
        public static void i(String format, Object... args) {
            // frame 2: skip frames 0 (#getClassName) and 1 (this method)
            Log.i(getClassName(2), String.format(format, args));
        }

        /**
         * The shim version of {@link Log#w(String, String)}.
         *
         * @param message The {@code String} to log
         */
        public static void w(String message) {
            // frame 2: skip frames 0 (#getClassName) and 1 (this method)
            Log.w(getClassName(2), message);
        }

        /**
         * The shim version of {@link Log#w(String, String)}.  Also calls String.format for
         * convenience.
         *
         * @param format A format string for the message to log
         * @param args The format string arguments
         */
        public static void w(String format, Object... args) {
            // frame 2: skip frames 0 (#getClassName) and 1 (this method)
            Log.w(getClassName(2), String.format(format, args));
        }

        /**
         * The shim version of {@link Log#e(String, String)}.
         *
         * @param message The {@code String} to log
         */
        public static void e(String message) {
            // frame 2: skip frames 0 (#getClassName) and 1 (this method)
            Log.e(getClassName(2), message);
        }

        /**
         * The shim version of {@link Log#e(String, String)}.  Also calls String.format for
         * convenience.
         *
         * @param format A format string for the message to log
         * @param args The format string arguments
         */
        public static void e(String format, Object... args) {
            // frame 2: skip frames 0 (#getClassName) and 1 (this method)
            Log.e(getClassName(2), String.format(format, args));
        }

        /**
         * The shim version of {@link Log#e(Throwable)}.
         *
         * @param t the {@link Throwable} to output.
         * @param args The format string arguments
         */
        public static void e(Throwable t) {
            // frame 2: skip frames 0 (#getClassName) and 1 (this method)
            Log.e(getClassName(2), t);
        }

        /**
         * The shim version of {@link Log#logAndDisplay(LogLevel, String, String)}.
         *
         * @param logLevel the {@link LogLevel}
         * @param format A format string for the message to log
         * @param args The format string arguments
         */
        public static void logAndDisplay(LogLevel logLevel, String format, Object... args) {
            // frame 2: skip frames 0 (#getClassName) and 1 (this method)
            Log.logAndDisplay(logLevel, getClassName(2), String.format(format, args));
        }

        /**
         * Return the simple classname from the {@code frame}th stack frame in the call path.
         * Note: this method does <emph>not</emph> check array bounds for the stack trace length.
         *
         * @param frame The index of the stack trace frame to inspect for the class name
         * @return The simple class name (or full-qualified if an error occurs getting a ref to the
         *         class) for the given element of the stack trace.
         */
        public static String getClassName(int frame) {
            StackTraceElement[] frames = (new Throwable()).getStackTrace();
            String fullName = frames[frame].getClassName();
            @SuppressWarnings("rawtypes")
            Class klass = null;
            try {
                klass = Class.forName(fullName);
            } catch (ClassNotFoundException e) {
                // oops; not much we can do.
                // Intentionally fall through so we hit the null check below
            }

            if (klass == null) {
                return fullName;
            } else {
                return klass.getSimpleName();
            }
        }
    }
}

