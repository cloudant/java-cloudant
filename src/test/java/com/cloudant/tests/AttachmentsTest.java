package com.cloudant.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Attachment;
import com.cloudant.client.api.model.Params;
import com.cloudant.client.api.model.Response;
import com.cloudant.tests.util.Utils;

public class AttachmentsTest {

	private static final Log log = LogFactory.getLog(AttachmentsTest.class);
	
	private static CloudantClient dbClient;
	private static Database db;
	private static Properties props ;

	@BeforeClass
	public static void setUpClass() {
		props = Utils.getProperties("cloudant.properties",log);
		dbClient = new CloudantClient(props.getProperty("cloudant.account"),
				  props.getProperty("cloudant.username"),
				  props.getProperty("cloudant.password"));
		db = dbClient.database("lightcouch-db-test", true);
	}

	@AfterClass
	public static void tearDownClass() {
		dbClient.shutdown();
	}

	@Test
	public void attachmentInline() {
		Attachment attachment1 = new Attachment("VGhpcyBpcyBhIGJhc2U2NCBlbmNvZGVkIHRleHQ=", "text/plain");

		Attachment attachment2 = new Attachment();
		attachment2.setData(Base64.encodeBase64String("binary string".getBytes()));
		attachment2.setContentType("text/plain");

		Bar bar = new Bar(); // Bar extends Document
		bar.addAttachment("txt_1.txt", attachment1);
		bar.addAttachment("txt_2.txt", attachment2);

		db.save(bar);
	}

	@Test
	public void attachmentInline_getWithDocument() {
		Attachment attachment = new Attachment("VGhpcyBpcyBhIGJhc2U2NCBlbmNvZGVkIHRleHQ=", "text/plain");
		Bar bar = new Bar();
		bar.addAttachment("txt_1.txt", attachment);
		
		Response response = db.save(bar);
		
		Bar bar2 = db.find(Bar.class, response.getId(), new Params().attachments());
		String base64Data = bar2.getAttachments().get("txt_1.txt").getData();
		assertNotNull(base64Data);
	}

	@Test
	public void attachmentStandalone() throws IOException {
		byte[] bytesToDB = "binary data".getBytes();
		ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
		Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain");

		InputStream in = db.find(response.getId() + "/foo.txt");
		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		int n;
		while ((n = in.read()) != -1) {
			bytesOut.write(n);
		}
		bytesOut.flush();
		in.close();

		byte[] bytesFromDB = bytesOut.toByteArray();

		assertArrayEquals(bytesToDB, bytesFromDB);
	}
}
