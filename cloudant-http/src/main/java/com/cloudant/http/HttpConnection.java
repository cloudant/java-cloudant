//  Copyright © 2015, 2017 IBM Corp. All rights reserved.
//
//  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
//  except in compliance with the License. You may obtain a copy of the License at
//  http://www.apache.org/licenses/LICENSE-2.0
//  Unless required by applicable law or agreed to in writing, software distributed under the
//  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
//  either express or implied. See the License for the specific language governing permissions
//  and limitations under the License.

package com.cloudant.http;

import com.cloudant.http.interceptors.BasicAuthInterceptor;
import com.cloudant.http.internal.DefaultHttpUrlConnectionFactory;
import com.cloudant.http.internal.Utils;
import com.cloudant.http.internal.interceptors.HttpConnectionInterceptorException;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


/**
 * Created by tomblench on 23/03/15.
 */

/**
 * <p>
 * A wrapper for <code>HttpURLConnection</code>s.
 * </p>
 *
 * <p>
 * Provides some convenience methods for making requests and sending/receiving data as streams,
 * strings, or byte arrays.
 * </p>
 *
 * <p>
 * Typical usage:
 * </p>
 *
 * <pre>
 * HttpConnection hc = new HttpConnection("POST", "application/json", new URL("http://somewhere"));
 * hc.requestProperties.put("x-some-header", "some-value");
 * hc.setRequestBody("{\"hello\": \"world\"});
 * String result = hc.execute().responseAsString();
 * // get the underlying HttpURLConnection if you need to do something a bit more advanced:
 * int response = hc.getConnection().getResponseCode();
 * hc.disconnect();
 * </pre>
 *
 * <p>
 * <b>Important:</b> this class is not thread-safe and <code>HttpConnection</code>s should not be
 * shared across threads.
 * </p>
 *
 * @see java.net.HttpURLConnection
 */
public class HttpConnection {

    private static final Logger logger = Logger.getLogger(HttpConnection.class.getCanonicalName());

    private final String requestMethod;
    public final URL url;
    private final String contentType;

    // The context
    private HttpConnectionInterceptorContext currentContext = null;

    // created in executeInternal
    private HttpURLConnection connection;

    // set by the various setRequestBody() methods
    private InputStreamGenerator input;
    private long inputLength;

    public final HashMap<String, String> requestProperties;

    public final List<HttpConnectionRequestInterceptor> requestInterceptors;
    public final List<HttpConnectionResponseInterceptor> responseInterceptors;

    /**
     * A connectionFactory for opening the URLs, can be set, but configured with a default
     */
    public HttpUrlConnectionFactory connectionFactory = new DefaultHttpUrlConnectionFactory();

    private int numberOfRetries = 10;
    private boolean requestIsLoggable = true;

    public HttpConnection(String requestMethod,
                          URL url,
                          String contentType) {
        this.requestMethod = requestMethod;
        this.url = url;
        this.contentType = contentType;
        this.requestProperties = new HashMap<String, String>();
        this.requestInterceptors = new LinkedList<HttpConnectionRequestInterceptor>();
        this.responseInterceptors = new LinkedList<HttpConnectionResponseInterceptor>();

        // Calculate log filter for this request if logging is enabled
        if (logger.isLoggable(Level.FINE)) {
            LogManager m = LogManager.getLogManager();
            String httpMethodFilter = m.getProperty("com.cloudant.http.filter.method");
            String urlFilter = m.getProperty("com.cloudant.http.filter.url");
            if (httpMethodFilter != null) {
                // Split the comma separated list of methods
                List<String> methods = Arrays.asList(httpMethodFilter.split(","));
                requestIsLoggable = requestIsLoggable && methods.contains(requestMethod);
            }
            if (urlFilter != null) {
                requestIsLoggable = requestIsLoggable && url.toString().matches(urlFilter);
            }
        }
    }

    /**
     * Sets the number of times this request can be attempted.
     * This method <strong>must</strong> be called before {@link #execute()}
     *
     * @param numberOfRetries the number of times this request can be attempted.
     * @return an {@link HttpConnection} for method chaining
     */
    public HttpConnection setNumberOfRetries(int numberOfRetries) {
        this.numberOfRetries = numberOfRetries;
        return this;
    }

    /**
     * @return the number of retries remaining for this request
     * @see #setNumberOfRetries(int)
     */
    public int getNumberOfRetriesRemaining() {
        return this.numberOfRetries;
    }

    /**
     * Set the String of request body data to be sent to the server.
     *
     * @param input String of request body data to be sent to the server
     * @return an {@link HttpConnection} for method chaining
     */
    public HttpConnection setRequestBody(final String input) {
        try {
            final byte[] inputBytes = input.getBytes("UTF-8");
            return setRequestBody(inputBytes);
        } catch (UnsupportedEncodingException e) {
            // This should never happen as every implementation of the java platform is required
            // to support UTF-8.
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the byte array of request body data to be sent to the server.
     *
     * @param input byte array of request body data to be sent to the server
     * @return an {@link HttpConnection} for method chaining
     */
    public HttpConnection setRequestBody(final byte[] input) {
        return setRequestBody(new ByteArrayInputStream(input), input.length);
    }

    /**
     * Set the InputStream of request body data to be sent to the server.
     *
     * @param input InputStream of request body data to be sent to the server
     * @return an {@link HttpConnection} for method chaining
     * @deprecated Use {@link #setRequestBody(InputStreamGenerator)}
     */
    public HttpConnection setRequestBody(final InputStream input) {
        // -1 signals inputLength unknown
        return setRequestBody(input, -1);
    }

    /**
     * Set the InputStream of request body data, of known length, to be sent to the server.
     *
     * @param input       InputStream of request body data to be sent to the server
     * @param inputLength Length of request body data to be sent to the server, in bytes
     * @return an {@link HttpConnection} for method chaining
     * @deprecated Use {@link #setRequestBody(InputStreamGenerator, long)}
     */
    public HttpConnection setRequestBody(final InputStream input, final long inputLength) {
        try {
            return setRequestBody(new InputStreamWrappingGenerator(input, inputLength),
                    inputLength);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error copying input stream for request body", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Set an InputStreamGenerator for an InputStream of request body data to be sent to the server.
     *
     * @param input InputStream of request body data to be sent to the server
     * @return an {@link HttpConnection} for method chaining
     * @since 2.4.0
     */
    public HttpConnection setRequestBody(final InputStreamGenerator input) {
        // -1 signals inputLength unknown
        return setRequestBody(input, -1);
    }

    /**
     * Set an InputStreamGenerator for an InputStream, of known length, of request body data to
     * be sent to the server.
     *
     * @param input       InputStreamGenerator that returns an InputStream of request body data
     *                    to be sent to the server
     * @param inputLength Length of request body data to be sent to the server, in bytes
     * @return an {@link HttpConnection} for method chaining
     * @since 2.4.0
     */
    public HttpConnection setRequestBody(final InputStreamGenerator input, final long inputLength) {
        this.input = input;
        this.inputLength = inputLength;
        return this;
    }

    /**
     * <p>
     * Execute request without returning data from server.
     * </p>
     * <p>
     * Call {@code responseAsString}, {@code responseAsBytes}, or {@code responseAsInputStream}
     * after {@code execute} if the response body is required.
     * </p>
     * <P>
     * Note if the URL contains user information it will be encoded in a BasicAuth header.
     * </P>
     *
     * @return An {@link HttpConnection} which can be used to obtain the response body
     * @throws IOException if there was a problem writing data to the server
     */
    public HttpConnection execute() throws IOException {
        boolean retry = true;

        while (retry && numberOfRetries-- > 0) {
            connection = connectionFactory.openConnection(url);

            if (url.getUserInfo() != null) {
                // Insert at position 0 in case another interceptor wants to overwrite the BasicAuth
                requestInterceptors.add(0, new BasicAuthInterceptor(url.getUserInfo()));
            }
            // always read the result, so we can retrieve the HTTP response code
            connection.setDoInput(true);
            connection.setRequestMethod(requestMethod);
            if (contentType != null) {
                connection.setRequestProperty("Content-type", contentType);
            }

            // We set up the output config before the interceptors to allow the configuration to be
            // modified. For example an interceptor might change the chunk size by calling
            // context.connection.getConnection().setChunkedStreamingMode(16384);
            if (input != null) {
                connection.setDoOutput(true);
                if (inputLength != -1) {
                    // TODO Remove this cast to int when the minimum supported level is 1.7.
                    // On 1.7 upwards this method takes a long, otherwise int.
                    connection.setFixedLengthStreamingMode((int) this.inputLength);
                } else {
                    connection.setChunkedStreamingMode(0); // Use 0 for the default size

                    // Note that CouchDB does not currently work for a chunked multipart stream, see
                    // https://issues.apache.org/jira/browse/COUCHDB-1403. Cases that use
                    // multipart need to provide the content length until that is fixed.
                }
            }

            currentContext = (currentContext == null) ? new HttpConnectionInterceptorContext
                    (this) : new HttpConnectionInterceptorContext(this, currentContext
                    .interceptorStates);

            for (HttpConnectionRequestInterceptor requestInterceptor : requestInterceptors) {
                currentContext = requestInterceptor.interceptRequest(currentContext);
            }

            //set request properties after interceptors, in case the interceptors have added
            // to the properties map
            for (Map.Entry<String, String> property : requestProperties.entrySet()) {
                connection.setRequestProperty(property.getKey(), property.getValue());
            }

            // Log the request
            if (requestIsLoggable && logger.isLoggable(Level.FINE)) {
                logger.fine(String.format("%s request%s", getLogRequestIdentifier(), (connection
                        .usingProxy() ? " via proxy" : "")));
            }

            // Log the request headers
            if (requestIsLoggable && logger.isLoggable(Level.FINER)) {
                logger.finer(String.format("%s request headers %s", getLogRequestIdentifier(),
                        connection.getRequestProperties()));
            }

            if (input != null) {
                InputStream is = input.getInputStream();
                OutputStream os = connection.getOutputStream();
                try {
                    // The buffer size used for writing to this output stream has an impact on the
                    //  HTTP chunk size, so we make it a pretty large size to avoid limiting the
                    // size
                    // of those chunks (although this appears in turn to set the chunk sizes).
                    IOUtils.copyLarge(is, os, new byte[16 * 1024]);
                    os.flush();
                } finally {
                    Utils.close(is);
                    Utils.close(os);
                }
            }

            // Log the response
            if (requestIsLoggable && logger.isLoggable(Level.FINE)) {
                logger.fine(String.format("%s response %s %s", getLogRequestIdentifier(),
                        connection.getResponseCode(), connection.getResponseMessage()));
            }

            // Log the response headers
            if (requestIsLoggable && logger.isLoggable(Level.FINER)) {
                logger.finer(String.format("%s response headers %s", getLogRequestIdentifier(),
                        connection.getHeaderFields()));
            }

            for (HttpConnectionResponseInterceptor responseInterceptor : responseInterceptors) {
                try {
                    currentContext = responseInterceptor.interceptResponse(currentContext);
                } catch (HttpConnectionInterceptorException e) {
                    // Sadly the current interceptor API doesn't allow an IOException to be thrown
                    // so to avoid swallowing them the interceptors need to wrap them in the runtime
                    // HttpConnectionInterceptorException and we can then unwrap them here.
                    Throwable cause = e.getCause();
                    if (cause != null && cause instanceof IOException) {
                        throw (IOException) cause;
                    } else {
                        throw e;
                    }
                }
            }

            // retry flag is set from the final step in the response interceptRequest pipeline
            retry = currentContext.replayRequest;

            // If we're going to retry we should consume any existing error streams to avoid
            // leaking connections. Consuming the stream is preferable to just closing it as it
            // makes the connection eligible for re-use.
            if (retry && numberOfRetries > 0) {
                Utils.consumeAndCloseStream(connection.getErrorStream());
            }

            if (numberOfRetries == 0) {
                logger.info("Maximum number of retries reached");
            }
        }
        // return ourselves to allow method chaining
        return this;
    }

    /**
     * <p>
     * Return response body data from server as a String.
     * </p>
     * <p>
     * <b>Important:</b> you must call <code>execute()</code> before calling this method.
     * </p>
     *
     * @return String of response body data from server, if any
     * @throws IOException if there was a problem reading data from the server
     */
    public String responseAsString() throws IOException {
        return IOUtils.toString(responseAsBytes(), "UTF-8");
    }

    /**
     * <p>
     * Return response body data from server as a byte array.
     * </p>
     * <p>
     * <b>Important:</b> you must call <code>execute()</code> before calling this method.
     * </p>
     *
     * @return Byte array of response body data from server, if any
     * @throws IOException if there was a problem reading data from the server
     */
    public byte[] responseAsBytes() throws IOException {
        InputStream is = responseAsInputStream();
        try {
            return IOUtils.toByteArray(is);
        } finally {
            Utils.close(is);
            disconnect();
        }

    }

    /**
     * <p>
     * Return response body data from server as an InputStream. The InputStream must be closed
     * after use to avoid leaking resources. Connection re-use may be improved if the entire stream
     * has been read before closing.
     * </p>
     * <p>
     * <b>Important:</b> you must call <code>execute()</code> before calling this method.
     * </p>
     *
     * @return InputStream of response body data from server, if any
     * @throws IOException if there was a problem reading data from the server
     */
    public InputStream responseAsInputStream() throws IOException {
        if (connection == null) {
            throw new IOException("Attempted to read response from server before calling execute" +
                    "()");
        }
        InputStream is = connection.getInputStream();
        return is;
    }

    /**
     * Get the underlying HttpURLConnection object, allowing clients to set/get properties not
     * exposed here.
     *
     * @return HttpURLConnection the underlying {@link HttpURLConnection} object
     */
    public HttpURLConnection getConnection() {
        return connection;
    }

    /**
     * Disconnect the underlying HttpURLConnection. Equivalent to calling:
     * <code>
     * getConnection.disconnect()
     * </code>
     */
    public void disconnect() {
        connection.disconnect();
    }

    /**
     * Factory used by HttpConnection to produce HttpUrlConnections.
     */
    public interface HttpUrlConnectionFactory {

        /**
         * Called by HttpConnection to open URLs, can be implemented to provide customization.
         *
         * @param url the address of the URL to open
         * @return HttpURLConnection for the specified URL
         * @throws IOException if there is an issue communicating with the server
         */
        HttpURLConnection openConnection(URL url) throws IOException;

        /**
         * Set a proxy server address. Note that this method must be called before {@link
         * HttpConnection#execute()} to have any effect.
         *
         * @param proxyUrl the URL of the HTTP proxy to use for this connection
         */
        void setProxy(URL proxyUrl);

        /**
         * Set an authenticator to be used to provide credentials for the connection to the
         * proxy server defined by the URL passed to {@link #setProxy(URL)}.
         *
         * @param proxyAuthentication the password authentication to use for the proxy connection
         */
        void setProxyAuthentication(PasswordAuthentication proxyAuthentication);

        /**
         * Give the connection provider an opportunity to clean up
         */
        void shutdown();
    }

    /**
     * An InputStreamGenerator has a single method getInputStream. Implementors return an
     * InputStream ready for consuming by the HttpConnection. The purpose of this is to
     * facilitate regeneration of an InputStream for cases where a payload is going to be resent
     * for a retry.
     *
     * @since 2.4.0
     */
    public interface InputStreamGenerator {

        /**
         * Implementors must return an InputStream that is ready to read from the beginning.
         * Implementors must not return the same InputStream instance from multiple calls
         * to this method unless the stream has been reset.
         *
         * @return an InputStream to use to read the body content for a HTTP request
         * @throws IOException if there is an error getting the InputStream
         */
        InputStream getInputStream() throws IOException;
    }

    /**
     * Implementation of InputStreamGenerator that checks if an InputStream is markable and performs
     * the necessary mark/reset required to do retries. If the supplied InputStream does not
     * support marking then it is copied into memory in a stream that does support marking.
     */
    private static final class InputStreamWrappingGenerator implements InputStreamGenerator {

        private final InputStream inputStream;

        InputStreamWrappingGenerator(InputStream inputStream, long size) throws IOException {
            if (!inputStream.markSupported()) {
                // If we can't mark/reset the stream then we read it into memory as a
                // ByteArrayInputStream so we are then able to mark/reset it for retries
                byte[] inputBytes = (size == -1) ? IOUtils.toByteArray(inputStream) : IOUtils
                        .toByteArray(inputStream, size);
                this.inputStream = new ByteArrayInputStream(inputBytes);
            } else {
                this.inputStream = inputStream;
            }
            // Now we should have a stream that supports marking, so mark it at the beginning,
            // ready for a reset if we need to retry later. Note use MAX_VALUE to allow as many
            // bytes as possible to be read before invalidating the mark.
            this.inputStream.mark(Integer.MAX_VALUE);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            // Reset the stream to the beginning
            this.inputStream.reset();
            return this.inputStream;
        }
    }

    private String logIdentifier = null;

    /**
     * Get a prefix for the log message to help identify which request is which and which responses
     * belong to which requests.
     */
    private String getLogRequestIdentifier() {
        if (logIdentifier == null) {
            logIdentifier = String.format("%s-%s %s %s", Integer.toHexString(hashCode()),
                    numberOfRetries, connection.getRequestMethod(), connection.getURL());
        }
        return logIdentifier;
    }
}
