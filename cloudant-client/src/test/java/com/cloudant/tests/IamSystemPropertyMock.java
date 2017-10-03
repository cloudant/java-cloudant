package com.cloudant.tests;

import org.junit.After;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Mock System.getProperty to fake the value of the IAM server
 *
 * Do this instead of setting the property in the real System because it pollutes the JVM
 */
public class IamSystemPropertyMock extends MockUp<System> {

    public final String mockIamTokenEndpointUrl;

    public IamSystemPropertyMock(String mockIamTokenEndpointUrl) {
        this.mockIamTokenEndpointUrl = mockIamTokenEndpointUrl;
    }

    @Mock
    public synchronized String getProperty(Invocation inv, String key) {
        if (key.equals("com.cloudant.client.iamserver")) {
            return mockIamTokenEndpointUrl;
        }
        return inv.proceed(key);
    }

    @Mock
    public synchronized String getProperty(Invocation inv, String key, String def) {
        if (key.equals("com.cloudant.client.iamserver")) {
            return mockIamTokenEndpointUrl;
        }
        return inv.proceed(key, def);
    }
}
