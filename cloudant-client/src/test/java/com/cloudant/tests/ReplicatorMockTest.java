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

import static com.cloudant.tests.HttpTest.takeN;
import static com.cloudant.tests.util.MockWebServerResources.EXPECTED_OK_COOKIE;
import static com.cloudant.tests.util.MockWebServerResources.EXPECTED_OK_COOKIE_2;
import static com.cloudant.tests.util.MockWebServerResources.JSON_OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.model.ReplicatorDocument;
import com.cloudant.tests.util.Utils;

import com.cloudant.tests.base.TestWithMockedServer;
import org.junit.jupiter.api.Test;

import okhttp3.mockwebserver.RecordedRequest;

import java.nio.charset.Charset;

/**
 * Created by samsmith on 08/03/2018.
 */

public class ReplicatorMockTest extends TestWithMockedServer {

    final private static String authJson = "\"auth\":{\"iam\":{\"api_key\":\"";
    final private static String replicatorDocId = Utils.generateUUID();

    final private static String sourceDbUrl = "https://foo.cloudant.com/source";
    final private static String targetDbUrl = "https://bar.cloudant.com/target";

    @Test
    public void createReplicationDocumentReconfigureSource() throws Exception {
        ReplicatorDocument rep = new ReplicatorDocument();

        rep.setSource(sourceDbUrl);
        assertEquals(rep.getSource(), sourceDbUrl);

        rep.setSourceIamApiKey(EXPECTED_OK_COOKIE);
        assertEquals(rep.getSource(), sourceDbUrl);
        assertEquals(rep.getSourceIamApiKey(), EXPECTED_OK_COOKIE);

        // reconfigure source

        rep.setSource(targetDbUrl);
        assertEquals(rep.getSource(), targetDbUrl);

        rep.setSourceIamApiKey(EXPECTED_OK_COOKIE_2);
        assertEquals(rep.getSourceIamApiKey(), EXPECTED_OK_COOKIE_2);
    }

    @Test
    public void createReplicationDocumentReconfigureTarget() throws Exception {
        ReplicatorDocument rep = new ReplicatorDocument();

        rep.setTarget(targetDbUrl);
        assertEquals(rep.getTarget(), targetDbUrl);

        rep.setTargetIamApiKey(EXPECTED_OK_COOKIE);
        assertEquals(rep.getTarget(), targetDbUrl);
        assertEquals(rep.getTargetIamApiKey(), EXPECTED_OK_COOKIE);

        // reconfigure target

        rep.setTarget(sourceDbUrl);
        assertEquals(rep.getTarget(), sourceDbUrl);

        rep.setTargetIamApiKey(EXPECTED_OK_COOKIE_2);
        assertEquals(rep.getTargetIamApiKey(), EXPECTED_OK_COOKIE_2);
    }

    @Test
    public void createReplicationReconfigureSourceSetIamApiKeyFirst() throws Exception {
        ReplicatorDocument rep = new ReplicatorDocument();

        rep.setSourceIamApiKey(EXPECTED_OK_COOKIE);
        assertEquals(rep.getSourceIamApiKey(), EXPECTED_OK_COOKIE);

        rep.setSource(sourceDbUrl);
        assertEquals(rep.getSource(), sourceDbUrl);
        assertEquals(rep.getSourceIamApiKey(), EXPECTED_OK_COOKIE);

        // reconfigure source

        rep.setSourceIamApiKey(EXPECTED_OK_COOKIE_2);
        assertEquals(rep.getSourceIamApiKey(), EXPECTED_OK_COOKIE_2);

        rep.setSource(targetDbUrl);
        assertEquals(rep.getSource(), targetDbUrl);
    }

    @Test
    public void createReplicationReconfigureTargetSetIamApiKeyFirst() throws Exception {
        ReplicatorDocument rep = new ReplicatorDocument();

        rep.setTargetIamApiKey(EXPECTED_OK_COOKIE);
        assertEquals(rep.getTargetIamApiKey(), EXPECTED_OK_COOKIE);

        rep.setTarget(targetDbUrl);
        assertEquals(rep.getTarget(), targetDbUrl);
        assertEquals(rep.getTargetIamApiKey(), EXPECTED_OK_COOKIE);

        // reconfigure target

        rep.setTarget(sourceDbUrl);
        assertEquals(rep.getTarget(), sourceDbUrl);

        rep.setTargetIamApiKey(EXPECTED_OK_COOKIE_2);
        assertEquals(rep.getTargetIamApiKey(), EXPECTED_OK_COOKIE_2);
    }

    @Test
    public void createAndSaveReplicatorDocumentWithNoIamAuth() throws Exception {
        server.enqueue(JSON_OK);

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(server)
                .build();

        c.replicator()
                .replicatorDocId(replicatorDocId)
                .source(sourceDbUrl)
                .target(targetDbUrl)
                .save();

        RecordedRequest[] requests = takeN(server, 1);

        assertEquals(requests[0].getPath(), "/_replicator/" + replicatorDocId);

        String body = requests[0].getBody().readUtf8();

        assertThat("The replication document should contain the correct source",
               body, containsString("\"source\":\"" + sourceDbUrl + "\""));

        assertThat("The replication document should contain the correct target",
                body, containsString("\"target\":\"" + targetDbUrl + "\""));
    }

    @Test
    public void createAndSaveReplicatorDocumentWithSourceIamAuth() throws Exception {
        server.enqueue(JSON_OK);

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(server)
                .build();

        c.replicator()
                .replicatorDocId(replicatorDocId)
                .source(sourceDbUrl)
                .sourceIamApiKey(EXPECTED_OK_COOKIE)
                .target(targetDbUrl)
                .save();

        RecordedRequest[] requests = takeN(server, 1);

        assertEquals(requests[0].getPath(), "/_replicator/" + replicatorDocId);

        String body = requests[0].getBody().readUtf8();

        assertThat("The replication document should contain the source IAM API key",
                body, containsString(authJson + EXPECTED_OK_COOKIE));

        assertThat("The replication document should contain the correct target",
                body, containsString("\"target\":\"" + targetDbUrl + "\""));
    }

    @Test
    public void createAndSaveReplicatorDocumentWithTargetIamAuth() throws Exception {
        server.enqueue(JSON_OK);

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(server)
                .build();

        c.replicator()
                .replicatorDocId(replicatorDocId)
                .source(sourceDbUrl)
                .target(targetDbUrl)
                .targetIamApiKey(EXPECTED_OK_COOKIE_2)
                .save();

        RecordedRequest[] requests = takeN(server, 1);

        assertEquals(requests[0].getPath(), "/_replicator/" + replicatorDocId);

        String body = requests[0].getBody().readUtf8();

        assertThat("The replication document should contain the source IAM API key",
                body, containsString("\"source\":\"" + sourceDbUrl + "\""));

        assertThat("The replication document should contain the correct target",
                body, containsString(authJson + EXPECTED_OK_COOKIE_2));
    }

    @Test
    public void createAndSaveReplicatorDocumentWithSourceAndTargetIamAuth() throws Exception {
        server.enqueue(JSON_OK);

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(server)
                .build();

        c.replicator()
                .replicatorDocId(replicatorDocId)
                .source(sourceDbUrl)
                .sourceIamApiKey(EXPECTED_OK_COOKIE)
                .target(targetDbUrl)
                .targetIamApiKey(EXPECTED_OK_COOKIE_2)
                .save();

        RecordedRequest[] requests = takeN(server, 1);

        assertEquals(requests[0].getPath(), "/_replicator/" + replicatorDocId);

        String body = requests[0].getBody().readUtf8();

        assertThat("The replication document should contain the source IAM API key",
                body, containsString(authJson + EXPECTED_OK_COOKIE));

        assertThat("The replication document should contain the target IAM API key",
                body, containsString(authJson + EXPECTED_OK_COOKIE_2));
    }
}
