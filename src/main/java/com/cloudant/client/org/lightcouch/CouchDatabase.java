package com.cloudant.client.org.lightcouch;

/**
 * <p>This class is the main object to use to gain access to a Database
 * @see CouchDatabaseBase
 */
public class CouchDatabase extends CouchDatabaseBase {

	CouchDatabase(CouchDbClientBase client, String name, boolean create) {
		super(client, name, create);
	}

}
