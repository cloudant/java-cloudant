/*
 * Copyright © 2017 IBM Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.cloudant.http.internal;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Utils {

    private static final Logger logger = Logger.getLogger(Utils.class.getName());

    /**
     * Closes a closeable (usually stream), suppressing any IOExceptions.
     *
     * @param closeable the resource to close quietly, may be {@code null}
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ioe) {
                // Suppress the exception
                logger.log(Level.FINEST, "Unable to close resource.", ioe);
            }
        }
    }

    /**
     * Consumes any available content from the stream (effectively sending it to /dev/null).
     *
     * @param is the stream to consume, may be {@code null}
     */
    public static void consumeStream(InputStream is) {
        if (is != null) {
            try {
                // Copy the stream to a null destination
                long copied = IOUtils.copyLarge(is, NullOutputStream.NULL_OUTPUT_STREAM);
                if (copied > 0) {
                    logger.log(Level.WARNING, "Consumed unused HTTP response error stream.");
                }
            } catch (IOException ioe) {
                // The stream was probably already closed, there's nothing further we can do anyway
                logger.log(Level.FINEST, "Unable to consume stream.", ioe);
            }
        }
    }

    /**
     * Equivalent to {@link Utils#consumeStream} followed by {@link Utils#close}.
     *
     * @param is - an input stream to consume and close, may be {@code null}
     */
    public static void consumeAndCloseStream(InputStream is) {
        try {
            consumeStream(is);
        } finally {
            close(is);
        }
    }

    /**
     * Collects the string content of a stream (UTF8) and then closes it.
     *
     * @param is - an input stream to consume and close, may be {@code null}
     * @return UTF-8 string content of the stream or null if there was no stream
     * @throws IOException if there was a problem reading the stream
     */
    public static String collectAndCloseStream(InputStream is) throws IOException {
        if (is != null) {
            try {
                return IOUtils.toString(is, "UTF-8");
            } finally {
                close(is);
            }
        } else {
            return null;
        }
    }
}
