package com.cloudant.tests.extensions;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

public class MultiExtension implements Extension, AfterAllCallback, AfterEachCallback,
        AfterTestExecutionCallback, BeforeAllCallback, BeforeEachCallback,
        BeforeTestExecutionCallback {

    private final Extension[] extensions;

    public MultiExtension(Extension... extensions) {
        this.extensions = extensions;
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        for (Extension extension : extensions) {
            if (extension instanceof AfterAllCallback) {
                ((AfterAllCallback) extension).afterAll(extensionContext);
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        for (Extension extension : extensions) {
            if (extension instanceof AfterEachCallback) {
                ((AfterEachCallback) extension).afterEach(extensionContext);
            }
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        for (Extension extension : extensions) {
            if (extension instanceof AfterTestExecutionCallback) {
                ((AfterTestExecutionCallback) extension).afterTestExecution(extensionContext);
            }
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        for (Extension extension : extensions) {
            if (extension instanceof BeforeAllCallback) {
                ((BeforeAllCallback) extension).beforeAll(extensionContext);
            }
        }
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        for (Extension extension : extensions) {
            if (extension instanceof BeforeEachCallback) {
                ((BeforeEachCallback) extension).beforeEach(extensionContext);
            }
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        for (Extension extension : extensions) {
            if (extension instanceof BeforeTestExecutionCallback) {
                ((BeforeTestExecutionCallback) extension).beforeTestExecution(extensionContext);
            }
        }
    }
}
