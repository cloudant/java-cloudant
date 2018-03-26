package com.cloudant.tests.base;

import com.cloudant.tests.extensions.DatabaseExtension;
import com.cloudant.tests.extensions.MultiExtension;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

// Base class for tests which require a DB which is used throughout the lifetime of the test class
public class TestWithDbPerClass extends TestWithDb {

    protected static DatabaseExtension.PerClass dbResource = new DatabaseExtension.PerClass(clientResource);

    @RegisterExtension
    protected static MultiExtension perClassExtensions = new MultiExtension(clientResource, dbResource);

    @BeforeAll
    public static void testWithDbBeforeAll() {
        account = clientResource.get();
        db = dbResource.get();
    }

}
