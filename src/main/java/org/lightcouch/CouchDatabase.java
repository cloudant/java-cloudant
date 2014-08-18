package org.lightcouch;

public class CouchDatabase extends CouchDatabaseBase {

	CouchDatabase(CouchDbClientBase client, String name, boolean create) {
		super(client, name, create);
	}

}
