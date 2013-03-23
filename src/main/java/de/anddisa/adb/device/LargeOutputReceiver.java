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

import com.android.ddmlib.IShellOutputReceiver;
import de.anddisa.adb.log.LogUtil.CLog;
import de.anddisa.adb.result.ByteArrayInputStreamSource;
import de.anddisa.adb.result.InputStreamSource;
import de.anddisa.adb.result.SnapshotInputStreamSource;
import de.anddisa.adb.util.FileUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;

/**
 * A class designed to help run long running commands collect output.
 * <p>
 * The maximum size of the tmp file is limited to approximately {@code maxFileSize}.
 * To prevent data loss when the limit has been reached, this file keeps two tmp host
 * files.
 * </p>
 */
public class LargeOutputReceiver implements IShellOutputReceiver {
    /** The max number of bytes to store in the buffer */
    public static final int BUFF_SIZE = 32 * 1024;

    private String mSerialNumber;
    private String mDescriptor;
    private long mMaxFileSize;

    private boolean mIsCancelled = false;
    private OutputStream mOutStream;
    /** the archived previous temp file */
    private File mPreviousTmpFile = null;
    /** the current temp file which data will be streamed into */
    private File mTmpFile = null;
    private long mTmpBytesStored = 0;

    /**
     * Creates a {@link LargeOutputReceiver}.
     *
     * @param descriptor the descriptor of the command to run. For logging only.
     * @param serialNumber the serial number of the device. For logging only.
     * @param maxFileSize the max file size of the tmp backing file in bytes.  Since the receiver
     * keeps two tmp host files, the size of the output can be up to twice {@code maxFileSize}.
     */
    public LargeOutputReceiver(String descriptor, String serialNumber, long maxFileSize) {
        mDescriptor = descriptor;
        mSerialNumber = serialNumber;
        mMaxFileSize = maxFileSize;

        try {
            createTmpFile();
        }  catch (IOException e) {
            CLog.w("failed to create %s file for %s.", mDescriptor, mSerialNumber);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void addOutput(byte[] data, int offset, int length) {
        if (mIsCancelled || mOutStream == null) {
            return;
        }
        try {
            mOutStream.write(data, offset, length);
            mTmpBytesStored += length;
            if (mTmpBytesStored > mMaxFileSize) {
                CLog.i("Max tmp %s file size reached for %s, swapping", mDescriptor, mSerialNumber);
                createTmpFile();
                mTmpBytesStored = 0;
            }
        } catch (IOException e) {
            CLog.w("failed to write %s data for %s.", mDescriptor, mSerialNumber);
        }
    }

    /**
     * Gets the collected output as a {@link InputStreamSource}.
     *
     * @return The collected output from the command.
     */
    public synchronized InputStreamSource getData() {
        if (mTmpFile != null) {
            flush();
            try {
                FileInputStream fileStream = new FileInputStream(mTmpFile);
                if (mPreviousTmpFile != null) {
                    // return an input stream that first reads from mPreviousTmpFile, then reads
                    // from mTmpFile
                    InputStream stream = new SequenceInputStream(
                            new FileInputStream(mPreviousTmpFile), fileStream);
                    return new SnapshotInputStreamSource(stream);
                } else {
                    // no previous file, just return a wrapper around mTmpFile's stream
                    return new SnapshotInputStreamSource(fileStream);
                }
            } catch (IOException e) {
                CLog.e("failed to get %s data for %s.", mDescriptor, mSerialNumber);
                CLog.e(e);
            }
        }

        // return an empty InputStreamSource
        return new ByteArrayInputStreamSource(new byte[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void flush() {
        if (mOutStream == null) {
            return;
        }
        try {
            mOutStream.flush();
        } catch (IOException e) {
            CLog.w("failed to flush %s data for %s.", mDescriptor, mSerialNumber);
        }
    }

    /**
     * Delete currently accumulated data, and then re-create a new file.
     */
    public synchronized void clear() {
        delete();

        try {
            createTmpFile();
        }  catch (IOException e) {
            CLog.w("failed to create %s file for %s.", mDescriptor, mSerialNumber);
        }
    }

    /**
     * Cancels the command.
     */
    public synchronized void cancel() {
        mIsCancelled = true;
    }

    /**
     * Delete all accumulated data.
     */
    public void delete() {
        flush();
        closeLogStream();

        FileUtil.deleteFile(mTmpFile);
        mTmpFile = null;
        FileUtil.deleteFile(mPreviousTmpFile);
        mPreviousTmpFile = null;
        mTmpBytesStored = 0;
    }

    /**
     * Closes the stream to tmp log file
     */
    private void closeLogStream() {
        try {
            if (mOutStream != null) {
                mOutStream.flush();
                mOutStream.close();
                mOutStream = null;
            }

        } catch (IOException e) {
            CLog.w("failed to close %s stream for %s.", mDescriptor, mSerialNumber);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isCancelled() {
        return mIsCancelled;
    }

    /**
     * Creates a new tmp file, closing the old one as necessary
     * <p>
     * Exposed for unit testing.
     * </p>
     * @throws IOException
     * @throws FileNotFoundException
     */
    synchronized void createTmpFile() throws IOException, FileNotFoundException {
        if (mIsCancelled) {
            CLog.w("Attempted to createTmpFile() after cancel() for device %s, ignoring.",
                    mSerialNumber);
            return;
        }

        closeLogStream();
        if (mPreviousTmpFile != null) {
            mPreviousTmpFile.delete();
        }
        mPreviousTmpFile = mTmpFile;
        mTmpFile = FileUtil.createTempFile(String.format("%s_%s_", mDescriptor, mSerialNumber),
                ".txt");
        CLog.i("Created tmp %s file %s", mDescriptor, mTmpFile.getAbsolutePath());
        mOutStream = new BufferedOutputStream(new FileOutputStream(mTmpFile),
                BUFF_SIZE);
        // add an initial message to log, to give info to viewer
        if (mPreviousTmpFile == null) {
            // first log!
            appendLogMsg(String.format("%s for device %s", mDescriptor, mSerialNumber));
        } else {
            appendLogMsg(String.format("Continuing %s capture for device %s. Previous content may "
                    + "have been truncated.", mDescriptor, mSerialNumber));
        }
    }

    /**
     * Adds a message to the captured device log.
     *
     * @param msg
     */
    protected synchronized void appendLogMsg(String msg) {
        if (mOutStream == null || msg == null) {
            return;
        }
        // add the msg to log, so readers will know the command was interrupted
        try {
            mOutStream.write("\n*******************\n".getBytes());
            mOutStream.write(msg.getBytes());
            mOutStream.write("\n*******************\n".getBytes());
        } catch (IOException e) {
            CLog.w("failed to write %s data for %s.", mDescriptor, mSerialNumber);
        }
    }

    /**
     * Get the descriptor.
     * <p>
     * Exposed for unit testing.
     * </p>
     */
    String getDescriptor() {
        return mDescriptor;
    }
}