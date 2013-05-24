package org.lightcouch.tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;

/**
 * {@link CouchDbProperties} creation test.
 *
 * <p> Run test:
 * <p> <tt>$ mvn test -Dtest=org.lightcouch.tests.CouchDbPropertiesTest</tt>
 *
 * @author Daan van Berkel
 *
 */

public class CouchDbPropertiesTest {
	private CouchDbProperties target;

	@Before
	public void createReferenceCouchDbProperties() {
		target = new CouchDbProperties("test", true, "http", "127.0.0.1", 5984, "", "");
	}

	@Test
	public void shouldBeAbleToUseSetters() {
		CouchDbProperties properties = new CouchDbProperties();
		properties.setDbName("test");
		properties.setCreateDbIfNotExist(true);
		properties.setProtocol("http");
		properties.setHost("127.0.0.1");
		properties.setPort(5984);
		properties.setUsername("");
		properties.setPassword("");

		assertEquals(properties, target);
	}

	@Test
	public void shouldBeAbleToUseFluentInterface() {
		CouchDbProperties properties = new CouchDbProperties()
		  .setDbName("test")
		  .setCreateDbIfNotExist(true)
		  .setProtocol("http")
		  .setHost("127.0.0.1")
		  .setPort(5984)
		  .setUsername("")
		  .setPassword("");

		assertEquals(properties, target);
	}

}
