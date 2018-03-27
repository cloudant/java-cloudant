/*
 * Copyright © 2018 IBM Corp. All rights reserved.
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
package com.cloudant.tests;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.internal.URIBase;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.extensions.CloudantClientExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;

@RequiresDB
public class URIBaseTest {

    @RegisterExtension
    public static CloudantClientExtension clientResource = new CloudantClientExtension();

    @Test
    public void buildAccountUri_noTrailingPathSeparator() throws Exception {
        CloudantClient client = ClientBuilder.url(clientResource.get().getBaseUri().toURL())
                .build();
        Assertions.assertFalse(client.getBaseUri().toString().endsWith("/"));
        URI clientUri = new URIBase(client.getBaseUri()).build();
        Assertions.assertFalse(clientUri.toString().endsWith("/"));

        //Check that path is not missing / separators
        clientUri = new URIBase(client.getBaseUri()).path("").path("api").path("couch").build();
        URI expectedAccountUri = new URI(clientResource.get().getBaseUri().toString()
                + "/api/couch");
        Assertions.assertEquals(expectedAccountUri, clientUri);
    }
}
