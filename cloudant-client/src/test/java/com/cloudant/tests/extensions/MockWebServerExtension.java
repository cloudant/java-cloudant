package com.cloudant.tests.extensions;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import okhttp3.mockwebserver.MockWebServer;

public class MockWebServerExtension implements BeforeEachCallback, AfterEachCallback {

    private MockWebServer mockWebServer;
    private boolean started = false;

    public MockWebServerExtension() {
    }

    public MockWebServer get() {
        return this.mockWebServer;
    }

    // NB beforeEach/afterEach emulate before and after in the actual mock, because we can't call
    // these directly as they are marked as protected

    @Override
    public synchronized void beforeEach(ExtensionContext context) throws Exception {
        this.mockWebServer = new MockWebServer();
        if (started) {
            System.err.println("*** WARNING: MockWebServer already started");
            return;
        }
        this.mockWebServer.start();
        started = true;
    }

    @Override
    public synchronized void afterEach(ExtensionContext context) throws Exception {
        System.out.println("MWS shutdown");
        this.mockWebServer.shutdown();
        started = false;
    }

}
