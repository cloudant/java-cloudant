/*
 * Copyright © 2019 IBM Corp. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.tests.base.TestWithMockedServer;

import org.junit.jupiter.api.Test;

import okhttp3.mockwebserver.MockResponse;

/**
 * Created by samsmith on 28/01/2019.
 */
public class PartitionInfoMockTests extends TestWithMockedServer {

    @Test
    public void getDbPartitionDocCount() {
        String partitionKey = "myPartitionKey";
        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(server).build();
        Database db = c.database("animaldb", false);

        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .setBody("{\"doc_count\":123}");
        server.enqueue(response);

        assertEquals(123, db.partitionInfo(partitionKey).getDocCount());
    }

    @Test
    public void getDbPartitionDeletedDocCount() {
        String partitionKey = "myPartitionKey";
        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(server).build();
        Database db = c.database("animaldb", false);

        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .setBody("{\"doc_del_count\":456}");
        server.enqueue(response);

        assertEquals(456, db.partitionInfo(partitionKey).getDocDelCount());
    }

    @Test
    public void getDbPartitionPartitionKey() {
        String partitionKey = "myPartitionKey";
        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(server).build();
        Database db = c.database("animaldb", false);

        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .setBody("{\"partition\":\"" + partitionKey + "\"}");
        server.enqueue(response);

        assertEquals(partitionKey, db.partitionInfo(partitionKey).getPartition());
    }

    @Test
    public void getDbPartitionSizesActive() {
        String partitionKey = "myPartitionKey";
        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(server).build();
        Database db = c.database("animaldb", false);

        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .setBody("{\"sizes\":{\"active\":1234}}");
        server.enqueue(response);

        assertEquals(1234, db.partitionInfo(partitionKey).getSizes().getActive());
    }

    @Test
    public void getDbPartitionSizesExternal() {
        String partitionKey = "myPartitionKey";
        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(server).build();
        Database db = c.database("animaldb", false);

        MockResponse response = new MockResponse()
                .setResponseCode(200)
                .setBody("{\"sizes\":{\"external\":5678}}");
        server.enqueue(response);

        assertEquals(5678, db.partitionInfo(partitionKey).getSizes().getExternal());
    }

}
