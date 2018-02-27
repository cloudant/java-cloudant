package com.cloudant.tests.base;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.tests.extensions.CloudantClientExtension;
import com.cloudant.tests.extensions.DatabaseExtension;
import com.cloudant.tests.extensions.MultiExtension;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

// Base class for tests which require a new DB for each test method
public class TestWithDbPerTest {

    protected static CloudantClientExtension clientResource = new CloudantClientExtension();
    protected static DatabaseExtension.PerTest dbResource = new DatabaseExtension.PerTest(clientResource);

    protected static CloudantClient account;
    protected static Database db;

    @RegisterExtension
    protected static MultiExtension perTestExtensions = new MultiExtension(clientResource, dbResource);

    @BeforeEach
    public void testWithDbBeforeEach() {
        db = dbResource.get();
    }

    @BeforeAll
    public static void testWithDbBeforeAll() {
        account = clientResource.get();
    }

}
