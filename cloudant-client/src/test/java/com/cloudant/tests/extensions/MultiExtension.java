/*
 * Copyright © 2018, 2019 IBM Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.cloudant.tests.extensions;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MultiExtension implements Extension, AfterAllCallback, AfterEachCallback,
        AfterTestExecutionCallback, BeforeAllCallback, BeforeEachCallback,
        BeforeTestExecutionCallback {

    private final List<Extension> extensions;
    private final List<Extension> reversedExtensions;

    public MultiExtension(Extension... extensions) {
        this.extensions = Arrays.asList(extensions);
        // After tests we need to call extensions in the reverse order
        this.reversedExtensions = new ArrayList<>(this.extensions.size());
        this.reversedExtensions.addAll(this.extensions);
        Collections.reverse(reversedExtensions);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        for (Extension extension : reversedExtensions) {
            if (extension instanceof AfterAllCallback) {
                ((AfterAllCallback) extension).afterAll(extensionContext);
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        for (Extension extension : reversedExtensions) {
            if (extension instanceof AfterEachCallback) {
                ((AfterEachCallback) extension).afterEach(extensionContext);
            }
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        for (Extension extension : reversedExtensions) {
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
