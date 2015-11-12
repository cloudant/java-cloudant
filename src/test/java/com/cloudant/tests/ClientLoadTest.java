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

package com.cloudant.tests;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.cloudant.tests.util.TestLog;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Ignore
public class ClientLoadTest {

    /**
     * client max connections
     */
    private static final int MAX_CONNECTIONS = 20;

    @ClassRule
    public static final TestLog log = new TestLog();

    public static CloudantClientResource clientResource = new CloudantClientResource
            (CloudantClientHelper.getClientBuilder()
            .maxConnections(MAX_CONNECTIONS));
    public static DatabaseResource dbResource = new DatabaseResource(clientResource);
    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(clientResource).around(dbResource);

    private static CloudantClient dbClient;
    private static Database db;

    @BeforeClass
    public static void setUp() {
        dbClient = clientResource.get();
        db = dbResource.get();
    }

    private static final int NUM_THREADS = 500;
    private static final int DOCS_PER_THREAD = 10;


    @After
    public void tearDown() {
        dbClient.shutdown();
    }

    @Test
    public void clientsLoadTest() {
        System.out.println("Load Test Started ...");

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        StopWatch.start();
        for (int i = 0; i < NUM_THREADS; i++) {
            executor.execute(new MyRunnable());
        }
        executor.shutdown();
        do { /* waiting */ }
        while (!executor.isTerminated());

        final long elapsed = StopWatch.stop();
        final long seconds = elapsed / 1000;
        final int totalDocs = NUM_THREADS * DOCS_PER_THREAD;

        printResult(elapsed, seconds, totalDocs);
    }

    private class MyRunnable implements Runnable {

        public void run() {
            for (int i = 0; i < DOCS_PER_THREAD; i++) {
                db.save(new Foo());
            }
        }
    }

    //

    private void printResult(long elapsed, long seconds, int totalDocs) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Load Test Completed:");
        sb.append("\n* Thread count: " + NUM_THREADS);
        sb.append("\n* Docs per thread: " + DOCS_PER_THREAD);
        sb.append("\n* Saved total new documents: " + totalDocs);
        sb.append(String.format("\n* Elapsed time: %s seconds, %s ms.", seconds, elapsed -
                (seconds * 1000)));
        sb.append("\n* Average persist time / document: " + (elapsed / totalDocs) + " ms.");
        System.out.println(sb);
    }

    // not thread safe
    static final class StopWatch {

        private StopWatch() {
        }

        private static long start;
        private static long stop;

        public static void start() {
            start = System.currentTimeMillis();
            stop = 0;
        }

        public static long stop() {
            stop = System.currentTimeMillis();
            long value = stop - start;
            start = 0;
            return value;
        }
    }

}

