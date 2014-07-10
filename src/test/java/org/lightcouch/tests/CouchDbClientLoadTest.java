/*
 * Copyright (C) 2011 lightcouch.org
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;

/**
 * {@link CouchDbClient} load test.
 * 
 * <p> Unignore test then run: <tt>$ mvn test -Dtest=org.lightcouch.tests.CouchDbClientLoadTest</tt>
 * 
 * @author ahmed
 *
 */
@Ignore
public class CouchDbClientLoadTest {
	
	private static CouchDbClient dbClient;

	private static final int NUM_THREADS     = 500; 
	private static final int DOCS_PER_THREAD = 10;
	
	/** client max connections */
	private static final int MAX_CONNECTIONS = 20;
	
	@BeforeClass 
	public static void setUpClass() {
		CouchDbProperties properties = new CouchDbProperties()
		  .setDbName("lightcouch-db-load")
		  .setCreateDbIfNotExist(true)
		  .setProtocol("http")
		  .setHost("127.0.0.1")
		  .setPort(5984)
		  .setMaxConnections(MAX_CONNECTIONS);
		
		dbClient = new CouchDbClient(properties);
	}
	
	@AfterClass
	public static void tearDownClass() {
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
				dbClient.save(new Foo());
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
    	sb.append(String.format("\n* Elapsed time: %s seconds, %s ms.", seconds, elapsed - (seconds * 1000)));
    	sb.append("\n* Average persist time / document: " + (elapsed / totalDocs) + " ms.");
    	System.out.println(sb);
	}
	
	// not thread safe
	static final class StopWatch {
		
		private StopWatch() {}
		
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

