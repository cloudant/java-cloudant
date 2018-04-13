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

package com.cloudant.tests.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.DesignDocumentManager;
import com.cloudant.client.api.model.DesignDocument;
import com.cloudant.client.api.model.ReplicatorDocument;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.api.scheduler.SchedulerDocsResponse;
import com.cloudant.client.internal.URIBase;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.http.HttpConnectionInterceptorContext;
import com.cloudant.http.HttpConnectionResponseInterceptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class Utils {

    //wait up to 2 minutes for replications to complete
    private static final long TIMEOUT_MILLISECONDS = TimeUnit.MINUTES.toMillis(2);
    //Design documents directory
    private static final File DESIGN_DOC_DIR
            = new File(System.getProperty("user.dir") + "/src/test/resources/design-files");

    public static void removeReplicatorTestDoc(CloudantClient account, String replicatorDocId)
            throws Exception {

        //Grab replicator doc revision using HTTP HEAD command
        String replicatorDb = "_replicator";
        URI uri = new URIBase(account.getBaseUri()).path(replicatorDb).path(replicatorDocId)
                .build();
        HttpConnection head = Http.HEAD(uri);

        //add a response interceptor to allow us to retrieve the ETag revision header
        final AtomicReference<String> revisionRef = new AtomicReference<String>();
        head.responseInterceptors.add(new HttpConnectionResponseInterceptor() {

            @Override
            public HttpConnectionInterceptorContext interceptResponse
                    (HttpConnectionInterceptorContext context) {
                revisionRef.set(context.connection.getConnection().getHeaderField("ETag"));
                return context;
            }
        });

        account.executeRequest(head);
        String revision = revisionRef.get();
        assertNotNull("The revision should not be null", revision);
        Database replicator = account.database(replicatorDb, false);
        Response removeResponse = replicator.remove(replicatorDocId,
                revision.replaceAll("\"", ""));

        assertThat(removeResponse.getError(), is(nullValue()));
    }

    public static String waitForReplicatorToStart(CloudantClient account,
                                                              String replicatorDocId)
            throws Exception {
        return waitForReplicatorToReachStatus(account, replicatorDocId, "triggered", "running");
    }

    public static String waitForReplicatorToComplete(CloudantClient account,
                                                                 String replicatorDocId)
            throws Exception {
        return waitForReplicatorToReachStatus(account, replicatorDocId, "completed");
    }

    public static String waitForReplicatorToReachStatus(CloudantClient account,
                                                                    String replicatorDocId,
                                                                    String... expectedStates)
            throws Exception {
        ReplicatorDocument replicatorDoc = null;

        long startTime = System.currentTimeMillis();
        long timeout = startTime + TIMEOUT_MILLISECONDS;
        //initial wait of 100 ms
        long delay = 100;

        while (System.currentTimeMillis() < timeout) {
            //Sleep before finding replication document
            Thread.sleep(delay);

            //Check if replicator doc is in specified state
            String state;

            if (account.metaInformation().getFeatures() != null &&
                    account.metaInformation().getFeatures().contains("scheduler")) {
                SchedulerDocsResponse.Doc doc = account.schedulerDoc(replicatorDocId);
                state = doc != null ? doc.getState() : null;
            } else {
                replicatorDoc = account.replicator()
                        .replicatorDocId(replicatorDocId)
                        .find();
                state = replicatorDoc != null ? replicatorDoc.getReplicationState() : null;
            }

            //if we've reached the status or we reached an error then we are finished
            if ("error".equalsIgnoreCase(state)) {
                return state;
            } else {
                for (String expectedState : expectedStates) {
                    if (expectedState.equalsIgnoreCase(state)) {
                        return state;
                    }
                }
            }
            //double the delay for the next iteration
            delay *= 2;
        }
        throw new TimeoutException("Timed out waiting for replication to complete");
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * <P>
     * When testing against a cluster requests can be routed to different nodes. This means after
     * a save a document may not appear for all subsequent requests until an internal replication
     * has finished between all the nodes. This method tries to find a document up to maxRetries
     * times, with retries separated by 0.5 s. Retrying may route the request to a node that
     * already has the document and the delay between retries also gives an opportunity for
     * internal replication to complete between nodes increasing the chance of retrieving the
     * document.
     * </P>
     * <P>
     * It should be noted that trying to read back a document immediately after writing to a cluster
     * is not a best practice, but is needed in some test cases to reproduce, for example,
     * conflict conditions. It is also worth noting that a document being returned from this
     * method is not a guarantee that it has reached all nodes, only that it has reached a node
     * that received one of the requests.
     * </P>
     *
     * @param db
     * @param docId
     */
    public static <T> T findDocumentWithRetries(Database db, String docId, Class<T> clazz, int
            maxRetries) throws Exception {
        for (int i = 1; i <= maxRetries; i++) {
            try {
                return db.find(clazz, docId);
            } catch (NoDocumentException e) {
                if (i == maxRetries) {
                    throw e;
                } else {
                    //sleep for 0.5 s before trying again
                    Thread.sleep(500);
                }
            }
        }
        throw new Exception("Retries exceeded");
    }

    /**
     * Test utility that splits a string and asserts that the expected number of separators were
     * found. Expected number is for separators, but the assertion is done on parts, so this
     * method is not safe for strings that end in their separator sequence.
     *
     * @param toSplit        the string to split
     * @param splitOn        the separator string
     * @param expectedNumber the expected number of separators
     * @return
     */
    public static String[] splitAndAssert(String toSplit, String splitOn, int expectedNumber) {
        String[] parts = toSplit.split(splitOn);
        assertEquals(expectedNumber + 1, parts.length, "There should be " + expectedNumber + " " +
                "instances of " + splitOn + " in the " +
                "content");
        return parts;
    }

    /**
     * Test utility to put design documents under the testing resource folder in to the database.
     *
     * @param db        database to put the design docs
     * @param directory location of design docs
     */
    public static void putDesignDocs(Database db, File directory) throws FileNotFoundException {
        //Get design documents from directory
        List<DesignDocument> designDocuments = DesignDocumentManager.fromDirectory(directory);
        DesignDocumentManager designDocumentManager = db.getDesignDocumentManager();

        DesignDocument[] designDocArray = designDocuments
                .toArray(new DesignDocument[designDocuments.size()]);
        //Put documents into database
        designDocumentManager.put(designDocArray);
    }

    public static void putDesignDocs(Database db) throws FileNotFoundException {
        putDesignDocs(db, DESIGN_DOC_DIR);
    }

    public static void assertOKResponse(Response r) throws Exception {
        assertTrue(r.getStatusCode()
                / 100 == 2, "The response code should be 2XX was " + r.getStatusCode());
    }
}
