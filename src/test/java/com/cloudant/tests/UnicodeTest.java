package com.cloudant.tests;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.SocketException;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Index;
import com.cloudant.client.api.model.IndexField;
import com.cloudant.client.api.model.IndexField.SortOrder;
import com.cloudant.test.main.RequiresCloudant;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UnicodeTest {

	private static final Log log = LogFactory.getLog(UnicodeTest.class);
	private static final String DB_NAME = "unicodetestdb";

	// According to JSON (ECMA-404, section 9 "Strings"):
	// - All Unicode characters except those that must be escaped
	//   (U+0000..U+001F, U+0022, U+005C) may be placed in a string.
	// - All Unicode characters may be included as Unicode escapes
	//   (after conversion to UTF-16).
	private static final String TESTSTRING = "Gr\u00fc\u00dfe \u65e5\u672c\u8a9e \uD834\uDD1E.";
	private static final String TESTSTRING_ESCAPED = "Gr\\u00fc\\u00dfe \\u65e5\\u672c\\u8a9e \\uD834\\uDD1E.";

	private static Database db;
	private static Properties props ;
	private CloudantClient account;

	@Before
	public void setup() {
		account = CloudantClientHelper.getClient();
		db = account.database(DB_NAME, true);
	}

	
	@After
	public void tearDown() {
		account.deleteDB(DB_NAME);
		account.shutdown();
	}

	// ========================================================================
	// REST request utilities.

	/**
	 * Returns the charset of a plain-text entity.
	 */
	private static Charset getPlainTextEntityCharset(HttpEntity entity, URI uri) {
		// For plain text, use the charset that is mentioned in the response
		// header field 'Content-Type'.
		// See http://stackoverflow.com/questions/3216730/with-httpclient-is-there-a-way-to-get-the-character-set-of-the-page-with-a-head
		ContentType contentType;
		try {
			contentType = ContentType.getOrDefault(entity);
		} catch (UnsupportedCharsetException e) {
			throw new RuntimeException("The output of "+uri+" is in charset "+e.getCharsetName()+", which is unsupported.",
									   e);
		}
		Charset charset = contentType.getCharset();
		if (charset == null) {
			// In the HTTP protocol, the default charset is ISO-8859-1.
			// But not for the Cloudant server:
			// - When we retrieve a document without specifying an 'Accept' header,
			//   it replies with a UTF-8 encoded JSON string and a header
			//   "Content-Type: text/plain;charset=utf-8".
			// - When we do the same thing with a "Accept: application/json" header,
			//   it replies with the same UTF-8 encoded JSON string and a header
			//   "Content-Type: application/json". So here the UTF-8 encoding must
			//   be implicitly understood.
			if ("application/json".equals(contentType.getMimeType())) {
				charset = Consts.UTF_8;
			} else {
				charset = HTTP.DEF_CONTENT_CHARSET;
			}
		}
		return charset;
	}

	/**
	 * Copies the content of an entity to an Appendable, as a sequence of chars.
	 * @param destination An Appendable (such as a StringBuilder, a Writer, or a PrintStream).
	 * @param reader A Reader that wraps the InputStream of the entity returned by the given URI.
	 *               Should be buffered.
	 * @throws RuntimeException if there is an exception reading the entity
	 * @throws IOException if there is an exception writing to the destination
	 */
	private static void pipeEntityContentAsChars(Appendable destination, Reader reader, URI uri) throws IOException {
		char[] buffer = new char[1024];
		for (;;) {
			int n;
			try {
				n = reader.read(buffer);
			} catch (SocketException e) {
				// At EOF, we may get this exception:
				// java.net.SocketException: Socket is closed
				//   at com.sun.net.ssl.internal.ssl.SSLSocketImpl.checkEOF(SSLSocketImpl.java:1284)
				//   at com.sun.net.ssl.internal.ssl.AppInputStream.read(AppInputStream.java:65)
				//   at org.apache.http.impl.io.AbstractSessionInputBuffer.fillBuffer(AbstractSessionInputBuffer.java:149)
				//   at org.apache.http.impl.io.SocketInputBuffer.fillBuffer(SocketInputBuffer.java:110)
				//   at org.apache.http.impl.io.AbstractSessionInputBuffer.readLine(AbstractSessionInputBuffer.java:264)
				//   at org.apache.http.impl.io.ChunkedInputStream.getChunkSize(ChunkedInputStream.java:246)
				//   at org.apache.http.impl.io.ChunkedInputStream.nextChunk(ChunkedInputStream.java:204)
				//   at org.apache.http.impl.io.ChunkedInputStream.read(ChunkedInputStream.java:167)
				//   at org.apache.http.conn.EofSensorInputStream.read(EofSensorInputStream.java:138)
				//   at java.io.BufferedInputStream.read1(BufferedInputStream.java:256)
				//   at java.io.BufferedInputStream.read(BufferedInputStream.java:317)
				//   at sun.nio.cs.StreamDecoder.readBytes(StreamDecoder.java:264)
				//   at sun.nio.cs.StreamDecoder.implRead(StreamDecoder.java:306)
				//   at sun.nio.cs.StreamDecoder.read(StreamDecoder.java:158)
				//   at java.io.InputStreamReader.read(InputStreamReader.java:167)
				//   at java.io.Reader.read(Reader.java:123)
				// See also http://stackoverflow.com/questions/17040698/httpcomponentss-ssl-connection-results-in-socket-is-closed
				break;
			} catch (IOException e) {
				throw new RuntimeException("Error reading from "+uri, e);
			}
			if (n < 0)
				break;
			destination.append(CharBuffer.wrap(buffer, 0, n));
		}
	}

	/**
	 * Outputs a plain-text entity to a character accumulator.
	 * @param destination An Appendable (such as a StringBuilder, a Writer, or a PrintStream).
	 * @throws RuntimeException if there is an exception reading the entity
	 * @throws IOException if there is an exception writing to the destination
	 */
	private static void pipePlainTextEntity(Appendable destination, HttpEntity entity, URI uri) throws IOException {
		Charset charset = getPlainTextEntityCharset(entity, uri);
		InputStream stream;
		try {
			stream = entity.getContent();
		} catch (IOException e) {
			throw new RuntimeException("Error starting to read from "+uri, e);
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
	 * @throws RuntimeException if there is an exception reading the entity
	 */
	private static String getPlainTextEntityAsString(HttpEntity entity, URI uri) {
		StringBuilder buf = new StringBuilder();
		try {
			pipePlainTextEntity(buf, entity, uri);
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
	private static JsonObject getJSONEntityAsJsonObject(HttpEntity entity, URI uri) {
		// Optimized: Avoids the use of a temporary string.
		Charset charset = getPlainTextEntityCharset(entity, uri);
		InputStream stream;
		try {
			stream = entity.getContent();
		} catch (IOException e) {
			throw new RuntimeException("Error starting to read from "+uri, e);
		}
		Reader reader = new InputStreamReader(new BufferedInputStream(stream), charset);
		return JSON_PARSER.parse(reader).getAsJsonObject();
	}

	/**
	 * Closes a REST response.
	 */
	private static void closeResponse(HttpResponse response) {
		if (response instanceof CloseableHttpResponse) {
			try {
				((CloseableHttpResponse)response).close();
			} catch (IOException e) {
				// We were only reading from a socket.
			}
		}
	}

	// ========================================================================

	/**
	 * Test whether literal Unicode characters in a string work.
	 */
	@Test
	public void testLiteralUnicode() {
		URI uri = URI.create(db.getDBUri()+"literal");
		{
			HttpPut request = new HttpPut(uri);
			request.addHeader("Accept", "application/json");
			HttpEntity entity = new StringEntity("{\"foo\":\""+TESTSTRING+"\"}", ContentType.APPLICATION_JSON); //$NON-NLS-1$ //$NON-NLS-2$
			request.setEntity(entity);
			HttpResponse response = account.executeRequest(request);
			assertEquals(201, response.getStatusLine().getStatusCode());
			closeResponse(response);
		}
		{
			HttpGet request = new HttpGet(uri);
			request.addHeader("Accept", "application/json");
			HttpResponse response = account.executeRequest(request);
			assertEquals(200, response.getStatusLine().getStatusCode());
			String result = getPlainTextEntityAsString(response.getEntity(), uri);
			System.out.println("testLiteralUnicode: Result as returned in entity: "+result);
			closeResponse(response);
		}
		{
			HttpGet request = new HttpGet(uri);
			request.addHeader("Accept", "application/json");
			HttpResponse response = account.executeRequest(request);
			assertEquals(200, response.getStatusLine().getStatusCode());
			JsonObject result = getJSONEntityAsJsonObject(response.getEntity(), uri);
			String value = result.get("foo").getAsString();
			assertEquals(TESTSTRING, value);
			closeResponse(response);
		}
	}

	/**
	 * Test whether escaped Unicode characters in a string work.
	 */
	@Test
	public void testEscapedUnicode() {
		URI uri = URI.create(db.getDBUri()+"escaped");
		{
			HttpPut request = new HttpPut(uri);
			request.addHeader("Accept", "application/json");
			HttpEntity entity = new StringEntity("{\"foo\":\""+TESTSTRING_ESCAPED+"\"}", ContentType.APPLICATION_JSON); //$NON-NLS-1$ //$NON-NLS-2$
			request.setEntity(entity);
			HttpResponse response = account.executeRequest(request);
			assertEquals(201, response.getStatusLine().getStatusCode());
			closeResponse(response);
		}
		{
			HttpGet request = new HttpGet(uri);
			request.addHeader("Accept", "application/json");
			HttpResponse response = account.executeRequest(request);
			assertEquals(200, response.getStatusLine().getStatusCode());
			String result = getPlainTextEntityAsString(response.getEntity(), uri);
			System.out.println("testEscapedUnicode: Result as returned in entity: "+result);
			closeResponse(response);
		}
		{
			HttpGet request = new HttpGet(uri);
			request.addHeader("Accept", "application/json");
			HttpResponse response = account.executeRequest(request);
			assertEquals(200, response.getStatusLine().getStatusCode());
			JsonObject result = getJSONEntityAsJsonObject(response.getEntity(), uri);
			String value = result.get("foo").getAsString();
			assertEquals(TESTSTRING, value);
			closeResponse(response);
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
	@Category(RequiresCloudant.class)
	public void testUnicodeInObject() {
		db.createIndex(
			"myview", "mydesigndoc", "json",
			new IndexField[] {
				new IndexField("foo", SortOrder.asc)
			});
		// Show the indices.
		for (Index index : db.listIndices()) {
			System.out.println(index);
		}
		// Create an object.
		MyObject object = new MyObject();
		object.foo = TESTSTRING;
		db.save(object);
		// We can now retrieve the matching documents through
		// GET /wladmin/_design/mydesigndoc/_view/myview?reduce=false&include_docs=true
		List<MyObject> result = db.view("mydesigndoc/myview").reduce(false).includeDocs(true).query(MyObject.class);
		assertEquals(1, result.size());
		String value = result.get(0).foo;
		assertEquals(TESTSTRING, value);
	}
}
