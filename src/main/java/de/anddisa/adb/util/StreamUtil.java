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
 */
package de.anddisa.adb.util;

import de.anddisa.adb.result.InputStreamSource;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for managing input streams.
 */
public class StreamUtil {

    private StreamUtil() {
    }

    /**
     * Retrieves a {@link String} from an {@link InputStreamSource}.
     *
     * @param source the {@link InputStreamSource}
     * @return a {@link String} containing the stream contents
     * @throws IOException if failure occurred reading the stream
     */
    public static String getStringFromSource(InputStreamSource source) throws IOException {
        final InputStream stream = source.createInputStream();
        final String contents;
        try {
            contents = getStringFromStream(stream);
        } finally {
            closeStream(stream);
        }
        return contents;
    }

    /**
     * Retrieves a {@link ByteArrayList} from an {@link InputStreamSource}.
     *
     * @param source the {@link InputStreamSource}
     * @return a {@link ByteArrayList} containing the stream contents
     * @throws IOException if failure occurred reading the stream
     */
    public static ByteArrayList getByteArrayListFromSource(InputStreamSource source)
            throws IOException {
        final InputStream stream = source.createInputStream();
        final ByteArrayList contents;
        try {
            contents = getByteArrayListFromStream(stream);
        } finally {
            closeStream(stream);
        }
        return contents;
    }

    /**
     * Retrieves a {@link String} from a character stream.
     *
     * @param stream the {@link InputStream}
     * @return a {@link String} containing the stream contents
     * @throws IOException if failure occurred reading the stream
     */
    public static String getStringFromStream(InputStream stream) throws IOException {
        Reader ir = new BufferedReader(new InputStreamReader(stream));
        int irChar = -1;
        StringBuilder builder = new StringBuilder();
        while ((irChar = ir.read()) != -1) {
            builder.append((char)irChar);
        }
        return builder.toString();
    }

    /**
     * Retrieves a {@link ByteArrayList} from a byte stream.
     *
     * @param stream the {@link InputStream}
     * @return a {@link ByteArrayList} containing the stream contents
     * @throws IOException if failure occurred reading the stream
     */
    public static ByteArrayList getByteArrayListFromStream(InputStream stream) throws IOException {
        InputStream is = new BufferedInputStream(stream);
        int inputByte = -1;
        ByteArrayList list = new ByteArrayList();
        while ((inputByte = is.read()) != -1) {
            list.add((byte)inputByte);
        }
        list.trimToSize();
        return list;
    }

    /**
     * Copies contents of origStream to destStream.
     * <p/>
     * Recommended to provide a buffered stream for input and output
     *
     * @param inStream the {@link InputStream}
     * @param outStream the {@link OutputStream}
     * @throws IOException
     */
    public static void copyStreams(InputStream inStream, OutputStream outStream)
            throws IOException {

        int data = -1;
        while ((data = inStream.read()) != -1) {
            outStream.write(data);
        }
    }

    /**
     * Copies contents of inStream to writer.
     * <p/>
     * Recommended to provide a buffered stream for input and output
     *
     * @param inStream the {@link InputStream}
     * @param writer the {@link Writer} destination
     * @throws IOException
     */
    public static void copyStreamToWriter(InputStream inStream, Writer writer) throws IOException {
        int data = -1;
        while ((data = inStream.read()) != -1) {
            writer.write(data);
        }
    }

    /**
     * Gets the stack trace as a {@link String}.
     *
     * @param throwable the {@link Throwable} to convert.
     * @return a {@link String} stack trace
     */
    public static String getStackTrace(Throwable throwable) {
        // dump the print stream results to the ByteArrayOutputStream, so contents can be evaluated
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream bytePrintStream = new PrintStream(outputStream);
        throwable.printStackTrace(bytePrintStream);
        return outputStream.toString();
    }

    /**
     * Closes given input stream.
     *
     * @param inStream the {@link InputStream}. No action taken if inStream is null.
     */
    public static void closeStream(InputStream inStream) {
        if (inStream != null) {
            try {
                inStream.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Closes given output stream.
     *
     * @param outStream the {@link OutputStream}. No action taken if outStream is null.
     */
    public static void closeStream(OutputStream outStream) {
        if (outStream != null) {
            try {
                outStream.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Attempts to flush the given output stream, and then closes it.
     *
     * @param outStream the {@link OutputStream}. No action taken if outStream is null.
     */
    public static void flushAndCloseStream(OutputStream outStream) {
        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                // ignore
            }
            try {
                outStream.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Closes given zip output stream.
     *
     * @param outStream the {@link ZipOutputStream}. No action taken if outStream is null.
     */
    public static void closeZipStream(ZipOutputStream outStream) {
        if (outStream != null) {
            try {
                outStream.closeEntry();
                outStream.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Closes given gzip output stream.
     *
     * @param outStream the {@link ZipOutputStream}. No action taken if outStream is null.
     */
    public static void closeGZipStream(GZIPOutputStream outStream) {
        if (outStream != null) {
            try {
                outStream.finish();
                outStream.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

}
