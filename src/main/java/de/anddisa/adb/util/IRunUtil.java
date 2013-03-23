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
package de.anddisa.adb.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Interface for running timed operations and system commands.
 */
public interface IRunUtil {

    /**
     * An interface for asynchronously executing an operation that returns a boolean status.
     */
    public static interface IRunnableResult {
        /**
         * Execute the operation.
         *
         * @return <code>true</code> if operation is performed successfully, <code>false</code>
         *         otherwise
         * @throws Exception if operation terminated abnormally
         */
        public boolean run() throws Exception;

        /**
         * Cancel the operation.
         */
        public void cancel();
    }

    /**
     * Sets the working directory for system commands.
     *
     * @param dir the working directory
     *
     * @see {@link ProcessBuilder#directory(File)}
     */
    public void setWorkingDir(File dir);

    /**
     * Sets a environment variable to be used when running system commands.
     *
     * @param name the variable name
     * @param value the variable value
     *
     * @see {@link ProcessBuilder#environment()}
     *
     */
    public void setEnvVariable(String key, String value);

    /**
     * Helper method to execute a system command, and aborting if it takes longer than a specified
     * time.
     *
     * @param timeout maximum time to wait in ms
     * @param command the specified system command and optionally arguments to exec
     * @return a {@link CommandResult} containing result from command run
     */
    public CommandResult runTimedCmd(final long timeout, final String... command);

    /**
     * Helper method to execute a system command, and aborting if it takes longer than a specified
     * time. Similar to {@link runTimedCmd}, but does not log any errors on exception.
     *
     * @param timeout maximum time to wait in ms
     * @param command the specified system command and optionally arguments to exec
     * @return a {@link CommandResult} containing result from command run
     */
    public CommandResult runTimedCmdSilently(final long timeout, final String... command);

    /**
     * Helper method to execute a system command that requires stdin input, and aborting if it
     * takes longer than a specified time.
     *
     * @param timeout maximum time to wait in ms
     * @param input the stdin input to pass to process
     * @param command the specified system command and optionally arguments to exec
     * @return a {@link CommandResult} containing result from command run
     */
    CommandResult runTimedCmdWithInput(long timeout, String input, String... command);

    /**
     * Helper method to execute a system command asynchronously.
     * <p/>
     * Will return immediately after launching command.
     *
     * @param command the specified system command and optionally arguments to exec
     * @return the {@link Process} of the executed command
     * @throws IOException if command failed to run
     */
    public Process runCmdInBackground(String... command) throws IOException;

    /**
     * An alternate {@link #runCmdInBackground(String...)} method that accepts the command arguments
     * in {@link List} form.
     *
     * @param command the {@link List} containing specified system command and optionally arguments
     *            to exec
     * @return the {@link Process} of the executed command
     * @throws IOException if command failed to run
     */
    public Process runCmdInBackground(List<String> command) throws IOException;

    /**
     * Block and executes an operation, aborting if it takes longer than a specified time.
     *
     * @param timeout maximum time to wait in ms
     * @param runnable {@link IRunUtil.IRunnableResult} to execute
     * @param to log errors on exception or not.
     * @return the {@link CommandStatus} result of operation.
     */
    public CommandStatus runTimed(long timeout, IRunUtil.IRunnableResult runnable,
            boolean logErrors);

    /**
     * Block and executes an operation multiple times until it is successful.
     *
     * @param opTimeout maximum time to wait in ms for one operation attempt
     * @param pollInterval time to wait between command retries
     * @param attempts the maximum number of attempts to try
     * @param runnable {@link IRunUtil.IRunnableResult} to execute
     * @return <code>true</code> if operation completed successfully before attempts reached.
     */
    public boolean runTimedRetry(long opTimeout, long pollInterval, int attempts,
            IRunUtil.IRunnableResult runnable);

    /**
     * Block and executes an operation multiple times until it is successful.
     *
     * @param opTimeout maximum time to wait in ms for a single operation attempt
     * @param pollInterval initial time to wait between operation attempts
     * @param maxTime the total approximate maximum time to keep trying the operation
     * @param runnable {@link IRunUtil.IRunnableResult} to execute
     * @return <code>true</code> if operation completed successfully before maxTime expired
     */
    public boolean runFixedTimedRetry(final long opTimeout, final long pollInterval,
            final long maxTime, final IRunUtil.IRunnableResult runnable);

    /**
     * Block and executes an operation multiple times until it is successful.
     * <p/>
     * Exponentially increase the wait time between operation attempts. This is intended to be used
     * when performing an operation such as polling a server, to give it time to recover in case it
     * is temporarily down.
     *
     * @param opTimeout maximum time to wait in ms for a single operation attempt
     * @param initialPollInterval initial time to wait between operation attempts
     * @param maxPollInterval the max time to wait between operation attempts
     * @param maxTime the total approximate maximum time to keep trying the operation
     * @param runnable {@link IRunUtil.IRunnableResult} to execute
     * @return <code>true</code> if operation completed successfully before maxTime expired
     */
    public boolean runEscalatingTimedRetry(final long opTimeout, final long initialPollInterval,
            final long maxPollInterval, final long maxTime, final IRunUtil.IRunnableResult
            runnable);

    /**
     * Helper method to sleep for given time, ignoring any exceptions.
     *
     * @param time ms to sleep. values less than or equal to 0 will be ignored
     */
    public void sleep(long time);
}
