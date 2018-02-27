package com.cloudant.tests.base;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.tests.extensions.CloudantClientExtension;
import com.cloudant.tests.extensions.DatabaseExtension;
import com.cloudant.tests.extensions.MultiExtension;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

// Base class for tests which require a DB which is used throughout the lifetime of the test class
public class TestWithDb {

    protected static CloudantClientExtension clientResource = new CloudantClientExtension();
    protected static DatabaseExtension.PerClass dbResource = new DatabaseExtension.PerClass(clientResource);

    protected static CloudantClient account;
    protected static Database db;

    @RegisterExtension
    protected static MultiExtension perClassExtensions = new MultiExtension(clientResource, dbResource);

    @BeforeAll
    public static void testWithDbBeforeAll() {
        account = clientResource.get();
        db = dbResource.get();
    }

}
