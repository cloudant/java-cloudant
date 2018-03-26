package com.cloudant.tests.base;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.tests.extensions.CloudantClientExtension;

public abstract class TestWithDb {

    protected static CloudantClientExtension clientResource = new CloudantClientExtension();

    protected static CloudantClient account;

    protected static Database db;

}
