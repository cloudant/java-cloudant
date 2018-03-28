/*
 * Copyright © 2015, 2018 IBM Corp. All rights reserved.
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

package com.cloudant.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cloudant.client.api.query.JsonIndex;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.internal.DatabaseURIHelper;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.base.TestWithDbPerTest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.SocketException;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiresDB
public class UnicodeTest extends TestWithDbPerTest {

    // According to JSON (ECMA-404, section 9 "Strings"):
    // - All Unicode characters except those that must be escaped
    //   (U+0000..U+001F, U+0022, U+005C) may be placed in a string.
    // - All Unicode characters may be included as Unicode escapes
    //   (after conversion to UTF-16).
    private static final String TESTSTRING_KEY = "teststring";
    private static final String TESTSTRING = "Gr\u00fc\u00dfe \u65e5\u672c\u8a9e \uD834\uDD1E.";
    private static final String TESTSTRING_ESCAPED = "Gr\\u00fc\\u00dfe \\u65e5\\u672c\\u8a9e " +
            "\\uD834\\uDD1E.";
    private static final String EXPECTED_JSON = "{\"_id\":\"" + TESTSTRING_KEY + "\"," +
            "\"_rev\":\"1-39933759c7250133b6039d94ea09134f\",\"foo\":\"Gr\u00fc\u00dfe " +
            "\u65e5\u672c\u8a9e \uD834\uDD1E.\"}\n";

    // ========================================================================
    // REST request utilities.

    /**
     * Returns the charset of a plain-text entity.
     */
    private static Charset getPlainTextEntityCharset(HttpConnection connection) {
        // For plain text, use the charset that is mentioned in the response
        // header field 'Content-Type'.
        // See http://stackoverflow.com/questions/3216730/with-httpclient-is-there-a-way-to-get
        // -the-character-set-of-the-page-with-a-head

        String contentType = connection.getConnection().getContentType();
        if (contentType == null) {
            contentType = "text/plain";
        }

        //look for any charset information in the Content-Type
        String charsetName = null;
        Matcher m = Pattern.compile(".*;\\s*charset=(^;)+.*", Pattern.CASE_INSENSITIVE).matcher
                (contentType);
        if (m.matches() && m.groupCount() >= 1) {
            charsetName = m.group(1);
        }

        Charset charset;
        if (charsetName == null) {
            // In the HTTP protocol, the default charset is ISO-8859-1.
            // But not for the Cloudant server:
            // - When we retrieve a document without specifying an 'Accept' header,
            //   it replies with a UTF-8 encoded JSON string and a header
            //   "Content-Type: text/plain;charset=utf-8".
            // - When we do the same thing with a "Accept: application/json" header,
            //   it replies with the same UTF-8 encoded JSON string and a header
            //   "Content-Type: application/json". So here the UTF-8 encoding must
            //   be implicitly understood.
            if ("application/json".equalsIgnoreCase(contentType)) {
                charset = Charset.forName("UTF-8");
            } else {
                charset = Charset.forName("ISO-8859-1");
            }
        } else {
            charset = Charset.forName(charsetName);
        }
        return charset;
    }

    /**
     * Copies the content of an entity to an Appendable, as a sequence of chars.
     *
     * @param destination An Appendable (such as a StringBuilder, a Writer, or a PrintStream).
     * @param reader      A Reader that wraps the InputStream of the entity returned by the given
     *                    URI.
     *                    Should be buffered.
     * @throws RuntimeException if there is an exception reading the entity
     * @throws IOException      if there is an exception writing to the destination
     */
    private static void pipeEntityContentAsChars(Appendable destination, Reader reader, URI uri)
            throws IOException {
        char[] buffer = new char[1024];
        for (; ; ) {
            int n;
            try {
                n = reader.read(buffer);
            } catch (SocketException e) {
                // At EOF, we may get this exception:
                // java.net.SocketException: Socket is closed
                //   at com.sun.net.ssl.internal.ssl.SSLSocketImpl.checkEOF(SSLSocketImpl.java:1284)
                //   at com.sun.net.ssl.internal.ssl.AppInputStream.read(AppInputStream.java:65)
                //   at org.apache.http.impl.io.AbstractSessionInputBuffer.fillBuffer
                // (AbstractSessionInputBuffer.java:149)
                //   at org.apache.http.impl.io.SocketInputBuffer.fillBuffer(SocketInputBuffer
                // .java:110)
                //   at org.apache.http.impl.io.AbstractSessionInputBuffer.readLine
                // (AbstractSessionInputBuffer.java:264)
                //   at org.apache.http.impl.io.ChunkedInputStream.getChunkSize
                // (ChunkedInputStream.java:246)
                //   at org.apache.http.impl.io.ChunkedInputStream.nextChunk(ChunkedInputStream
                // .java:204)
                //   at org.apache.http.impl.io.ChunkedInputStream.read(ChunkedInputStream.java:167)
                //   at org.apache.http.conn.EofSensorInputStream.read(EofSensorInputStream
                // .java:138)
                //   at java.io.BufferedInputStream.read1(BufferedInputStream.java:256)
                //   at java.io.BufferedInputStream.read(BufferedInputStream.java:317)
                //   at sun.nio.cs.StreamDecoder.readBytes(StreamDecoder.java:264)
                //   at sun.nio.cs.StreamDecoder.implRead(StreamDecoder.java:306)
                //   at sun.nio.cs.StreamDecoder.read(StreamDecoder.java:158)
                //   at java.io.InputStreamReader.read(InputStreamReader.java:167)
                //   at java.io.Reader.read(Reader.java:123)
                // See also http://stackoverflow
                // .com/questions/17040698/httpcomponentss-ssl-connection-results-in-socket-is
                // -closed
                break;
            } catch (IOException e) {
                throw new RuntimeException("Error reading from " + uri, e);
            }
            if (n < 0) {
                break;
            }
            destination.append(CharBuffer.wrap(buffer, 0, n));
        }
    }

    /**
     * Outputs a plain-text entity to a character accumulator.
     *
     * @param destination An Appendable (such as a StringBuilder, a Writer, or a PrintStream).
     * @throws RuntimeException if there is an exception reading the entity
     * @throws IOException      if there is an exception writing to the destination
     */
    private static void pipePlainTextEntity(Appendable destination, HttpConnection connection,
                                            URI uri)
            throws IOException {
        Charset charset = getPlainTextEntityCharset(connection);
        InputStream stream;
        try {
            stream = connection.responseAsInputStream();
        } catch (IOException e) {
            throw new RuntimeException("Error starting to read from " + uri, e);
        }
        Reader reader = new InputStreamReader(new BufferedInputStream(stream), charset);
        try {
            pipeEntityContentAsChars(destination, reader, uri);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // Ignore errors. We were only reading from a socket.
            }
        }
    }

    /**
     * Returns a plain-text entity as a string.
     *
     * @throws RuntimeException if there is an exception reading the entity
     */
    private static String getPlainTextEntityAsString(HttpConnection connection, URI uri) {
        StringBuilder buf = new StringBuilder();
        try {
            pipePlainTextEntity(buf, connection, uri);
        } catch (IOException e) {
            // Shouldn't happen.
            throw new RuntimeException(e);
        }
        return buf.toString();
    }

    /**
     * The JsonParser instance to reuse each time parseAsJson method is invoked
     */
    static private final JsonParser JSON_PARSER = new JsonParser();

    /**
     * Returns a JSON entity as a JSON object.
     */
    private static JsonObject getJSONEntityAsJsonObject(HttpConnection connection, URI uri) {
        // Optimized: Avoids the use of a temporary string.
        Charset charset = getPlainTextEntityCharset(connection);
        InputStream stream;
        try {
            stream = connection.responseAsInputStream();
        } catch (IOException e) {
            throw new RuntimeException("Error starting to read from " + uri, e);
        }
        Reader reader = new InputStreamReader(new BufferedInputStream(stream), charset);
        return JSON_PARSER.parse(reader).getAsJsonObject();
    }

    /**
     * Closes a REST response.
     */
    private static void closeResponse(HttpConnection response) throws Exception {
        InputStream responseStream = response.responseAsInputStream();
        IOUtils.closeQuietly(responseStream);
    }

    // ========================================================================

    /**
     * Test whether literal Unicode characters in a string work.
     */
    @Test
    public void testLiteralUnicode() throws Exception {
        URI uri = new DatabaseURIHelper(db.getDBUri()).path(TESTSTRING_KEY).build();
        {
            HttpConnection conn = Http.PUT(uri, "application/json");
            conn.requestProperties.put("Accept", "application/json");
            conn.setRequestBody("{\"foo\":\"" + TESTSTRING + "\"}");
            clientResource.get().executeRequest(conn);
            assertEquals(2, conn.getConnection().getResponseCode() / 100);
            closeResponse(conn);
        }
        {
            HttpConnection conn = Http.GET(uri);
            conn.requestProperties.put("Accept", "application/json");
            clientResource.get().executeRequest(conn);
            assertEquals(200, conn.getConnection().getResponseCode());
            String result = getPlainTextEntityAsString(conn, uri);
            assertEquals(EXPECTED_JSON, result);
            closeResponse(conn);
        }
        {
            HttpConnection conn = Http.GET(uri);
            conn.requestProperties.put("Accept", "application/json");
            clientResource.get().executeRequest(conn);
            assertEquals(200, conn.getConnection().getResponseCode());
            JsonObject result = getJSONEntityAsJsonObject(conn, uri);
            String value = result.get("foo").getAsString();
            assertEquals(TESTSTRING, value);
            closeResponse(conn);
        }
    }

    /**
     * Test whether escaped Unicode characters in a string work.
     */
    @Test
    public void testEscapedUnicode() throws Exception {
        URI uri = new DatabaseURIHelper(db.getDBUri()).path(TESTSTRING_KEY).build();
        {
            HttpConnection conn = Http.PUT(uri, "application/json");
            conn.requestProperties.put("Accept", "application/json");
            conn.setRequestBody("{\"foo\":\"" + TESTSTRING_ESCAPED + "\"}");
            clientResource.get().executeRequest(conn);
            assertEquals(2, conn.getConnection().getResponseCode() / 100);
            closeResponse(conn);
        }
        {
            HttpConnection conn = Http.GET(uri);
            conn.requestProperties.put("Accept", "application/json");
            clientResource.get().executeRequest(conn);
            assertEquals(200, conn.getConnection().getResponseCode());
            String result = getPlainTextEntityAsString(conn, uri);
            assertEquals(EXPECTED_JSON, result);
            closeResponse(conn);
        }
        {
            HttpConnection conn = Http.GET(uri);
            conn.requestProperties.put("Accept", "application/json");
            clientResource.get().executeRequest(conn);
            assertEquals(200, conn.getConnection().getResponseCode());
            JsonObject result = getJSONEntityAsJsonObject(conn, uri);
            String value = result.get("foo").getAsString();
            assertEquals(TESTSTRING, value);
            closeResponse(conn);
        }
    }

    public static class MyObject {
        public String foo;
    }

    /**
     * Test whether Unicode characters in a Java object work.
     */
    // This test used to fail if the default encoding of the current JVM is not UTF-8.
    // To reproduce: In Eclipse, use "Run > Run Configurations...", tab "Common",
    // panel "Encoding", set the encoding to ISO-8859-1.
    @Test
    @RequiresCloudant
    public void testUnicodeInObject() throws Exception {
        db.createIndex(JsonIndex.builder()
                .name("myview")
                .designDocument("mydesigndoc")
                .asc("foo")
                .definition());

        // Create an object.
        MyObject object = new MyObject();
        object.foo = TESTSTRING;
        db.save(object);
        // We can now retrieve the matching documents through
        // GET /wladmin/_design/mydesigndoc/_view/myview?reduce=false&include_docs=true
        List<MyObject> result = db.getViewRequestBuilder("mydesigndoc", "myview").newRequest(Key
                .Type.STRING, String.class).reduce(false).includeDocs(true).build().getResponse()
                .getDocsAs(MyObject.class);
        assertEquals(1, result.size());
        String value = result.get(0).foo;
        assertEquals(TESTSTRING, value);
    }
}
