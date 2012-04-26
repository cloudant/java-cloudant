/*
 * Copyright (C) 2011 Ahmed Yehia (ahmed.yehia.m@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lightcouch.tests;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lightcouch.CouchDbClient;

/**
 * Clients load test.
 * 
 * <p> Run test:  
 * <p> <tt>$ mvn test -Dtest=org.lightcouch.tests.CouchDbClientLoadTest</tt>
 * 
 * @author Ahmed Yehia
 *
 */
public class CouchDbClientLoadTest {
	
	private static CouchDbClient dbClient;
	private static CouchDbClient dbClient2;

	private static final int NUM_THREADS = 1 * 2; // n threads * 2 clients
	private static final int NUM_DOCS = 1; // n docs per thread
	
	@BeforeClass 
	public static void setUpClass() {
		dbClient = new CouchDbClient();
		dbClient2 = new CouchDbClient("couchdb-2.properties"); 
	}
	
	@AfterClass
	public static void tearDownClass() {
		dbClient.shutdown();
		dbClient2.shutdown();
	}
	
	@Test
	public void clientsLoadTest() {
		// init
    	ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
    	final MyRunnable[] runnables = new MyRunnable[NUM_THREADS];
    	for (int i = 0; i < NUM_THREADS; i += 2) {
    		runnables[i] = new MyRunnable(dbClient, new Foo(UUID.randomUUID().toString().replace("-", "")));
    		runnables[i+1] = new MyRunnable(dbClient2, new Foo(UUID.randomUUID().toString().replace("-", "")));
    	}
    	
    	// start
    	System.out.println("--------------- Load Test Running ...");
    	StopWatch.start();
    	for (int i = 0; i < NUM_THREADS; i++) {
    		executor.execute(runnables[i]);
    	}
    	executor.shutdown();
    	do { /* waiting */ } 
    		while (!executor.isTerminated());
    	
    	// analyse
    	long elapsed = StopWatch.stop();
    	int totalObjects = (NUM_THREADS * NUM_DOCS);
    	StringBuilder sb = new StringBuilder();
    	sb.append("--------------- Load Test Ended:");
    	sb.append("\n* Thread count = " + NUM_THREADS);
    	sb.append("\n* Docs per thread = " + NUM_DOCS);
    	sb.append("\n* Saved total new objects = " + totalObjects);
    	sb.append(String.format("\n* Elapsed time: %s seconds, %s ms", (elapsed / 1000), elapsed - (( elapsed / 1000) * 1000)));
    	sb.append("\n* Average object saving time = " + (elapsed / totalObjects) + " ms");
    	System.out.println(sb);
	}
	
	private class MyRunnable implements Runnable {
		CouchDbClient client;
		Foo foo;
		
		public MyRunnable(CouchDbClient client, Foo foo) {
			this.client = client;
			this.foo = foo;
		}

		public void run() {
			for (int i = 0; i < NUM_DOCS; i++) {
				foo.set_id(foo.get_id() + i);
				client.save(foo);
			}
		}
	}
}

