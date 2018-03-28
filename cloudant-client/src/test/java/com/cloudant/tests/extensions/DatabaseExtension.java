/*
 * Copyright © 2015, 2018 IBM Corp. All rights reserved.
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

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.tests.util.Utils;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseExtension {

    // Get the maximum length for a database name, defaults to 128 chars
    private static final int DB_NAME_SIZE = Integer.parseInt(System.getProperty("test.db.name" +
            ".length", "128"));
    private final AbstractClientExtension clientResource;

    private CloudantClient client;
    private String databaseName = Utils.generateUUID();
    private Database database;
    private boolean mock = false;

    /**
     * Create a database resource from the specified clientResource. Note that if the resources
     * are at the same level (i.e. Rule or ClassRule) then a RuleChain is required to guarantee
     * the client is set up before the database e.g.
     * <pre>
     * {@code
     *    public static final CloudantClientResource clientResource = new CloudantClientResource();
     *    public static final DatabaseResource dbResource = new DatabaseResource(clientResource);
     *    @ClassRule
     *    public static final RuleChain CHAIN = RuleChain.outerRule(clientResource).around
     *    (dbResource);
     * }
     * </pre>
     *
     * @param clientResource
     */

    protected DatabaseExtension(CloudantClientExtension clientResource) {
        this.clientResource = clientResource;
    }

    protected DatabaseExtension(CloudantClientMockServerExtension mockServerResource) {
        this.clientResource = mockServerResource;
        this.mock = true;
    }

    /**
     * The database must start with a letter and can only contain lowercase letters (a-z), digits
     * (0-9) and the following characters _, $, (, ), +, -, and /.
     *
     * @param name to sanitize
     * @return sanitized name
     */
    private static String sanitizeDbName(String name) {
        //lowercase to remove any caps that will not be permitted
        name = name.toLowerCase(Locale.ENGLISH);
        //replace any non alphanum with underscores
        name = name.replaceAll("[^a-z0-9]", "_");
        //squash multiple underscores
        name = name.replaceAll("(_){2,}", "_");
        //remove leading underscore as it is reserved for internal couch use
        name = name.replaceAll("^_", "");
        //database name is limited to 128 characters on the Cloudant service
        //forego the package name in favour of test details and UUID
        int excess;
        if ((excess = name.length() - DB_NAME_SIZE) > 0) {
            name = name.substring(excess, name.length());
            //if the new name doesn't start with a letter use the bit from the first letter
            Matcher m = Pattern.compile("[^a-z](.*)").matcher(name);
            if (m.matches()) {
                name = m.group(1);
            }
        }
        return name;
    }

    public void before(ExtensionContext context) {
        String uniqueSuffix = Utils.generateUUID();
        databaseName = sanitizeDbName(String.format("%s-%s", context.getUniqueId(), uniqueSuffix));
        client = clientResource.get();
        if (!mock) {
            database = client.database(databaseName, true);
        } else {
            database = client.database(databaseName, false);
        }
    }

    public void after(ExtensionContext context) {
        if (!mock) {
            client.deleteDB(databaseName);
        }
    }

    public Database get() {
        return this.database;
    }

    public String getDatabaseName() {
        return this.databaseName;
    }

    /**
     * Get a string representation of the URI for the specified DB resource that includes
     * credentials. This is needed for replication cases where the DB needs to be able to obtain
     * creds for the DB.
     *
     * @return the URI for the DB with creds
     */
    public String getDbURIWithUserInfo() throws Exception {
        String info = clientResource.getBaseURIWithUserInfo() + "/" + getDatabaseName();
        return info;
    }

    public static class PerClass extends DatabaseExtension implements BeforeAllCallback,
            AfterAllCallback {

        public PerClass(CloudantClientExtension clientResource) {
            super(clientResource);
        }

        @Override
        public void afterAll(ExtensionContext extensionContext) throws Exception {
            super.after(extensionContext);
        }

        @Override
        public void beforeAll(ExtensionContext extensionContext) throws Exception {
            super.before(extensionContext);
        }
    }

    public static class PerTest extends DatabaseExtension implements BeforeEachCallback,
            AfterEachCallback {

        public PerTest(CloudantClientExtension clientResource) {
            super(clientResource);
        }

        public PerTest(CloudantClientMockServerExtension mockServerResource) {
            super(mockServerResource);
        }

        @Override
        public void afterEach(ExtensionContext extensionContext) throws Exception {
            super.after(extensionContext);
        }

        @Override
        public void beforeEach(ExtensionContext extensionContext) throws Exception {
            super.before(extensionContext);
        }
    }
}
