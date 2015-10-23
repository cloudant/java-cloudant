/*
 * Copyright (c) 2015 IBM Corp. All rights reserved.
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

package com.cloudant.tests.util;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Locale;

public class DatabaseResource extends ExternalResource {

    private final CloudantClientResource clientResource;
    private CloudantClient client;
    private String databaseName = Utils.generateUUID();
    private Database database;

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
    public DatabaseResource(CloudantClientResource clientResource) {
        this.clientResource = clientResource;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        String testClassName = description.getClassName();
        String testMethodName = description.getMethodName();
        String uniqueSuffix = Utils.generateUUID();
        databaseName = sanitizeDbName(testClassName + "-" + (testMethodName == null ? "" :
                testMethodName + "-") + uniqueSuffix);
        return super.apply(base, description);
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
        //replace any characters that are not permitted with underscores
        name = name.replaceAll("[^a-z0-9_\\$\\(\\)\\+\\-/]", "_");
        return name;
    }

    @Override
    public void before() {
        client = clientResource.get();
        database = client.database(databaseName, true);
    }

    @Override
    public void after() {
        client.deleteDB(databaseName);
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
        return clientResource.getBaseURIWithUserInfo() + "/" + getDatabaseName();
    }
}
