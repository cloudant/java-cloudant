/*
 * Copyright (C) 2011 lightcouch.org
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

package com.cloudant.client.org.lightcouch;

import com.cloudant.client.internal.DatabaseURIHelper;
import com.cloudant.client.org.lightcouch.ChangesResult.Row;
import com.cloudant.client.org.lightcouch.internal.CouchDbUtil;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;

/**
 * <p>Contains the Change Notifications API, supports <i>normal</i> and <i>continuous</i> feed
 * Changes.
 * <h3>Usage Example:</h3>
 * <pre>
 * // feed type normal
 * String since = db.info().getUpdateSeq(); // latest update seq
 * ChangesResult changeResult = db.changes()
 * 	.since(since)
 * 	.limit(10)
 * 	.filter("example/filter")
 * 	.getChanges();
 *
 * for (ChangesResult.Row row : changeResult.getResults()) {
 *   String docId = row.getId()
 *   JsonObject doc = row.getDoc();
 * }
 *
 * // feed type continuous
 * Changes changes = db.changes()
 * 	.includeDocs(true)
 * 	.heartBeat(30000)
 * 	.continuousChanges();
 *
 * while (changes.hasNext()) {
 * 	ChangesResult.Row feed = changes.next();
 *  String docId = feed.getId();
 *  JsonObject doc = feed.getDoc();
 * 	// changes.stop(); // stop continuous feed
 * }
 * </pre>
 *
 * @author Ahmed Yehia
 * @see ChangesResult
 * @since 0.0.2
 */
public class Changes {

    private BufferedReader reader;
    private Row nextRow;
    private boolean stop;

    private CouchDatabaseBase dbc;
    private Gson gson;
    private DatabaseURIHelper databaseHelper;

    Changes(CouchDatabaseBase dbc) {
        this.dbc = dbc;
        this.gson = dbc.couchDbClient.getGson();
        this.databaseHelper = new DatabaseURIHelper(dbc.getDBUri());
    }

    /**
     * Requests Change notifications of feed type continuous.
     * <p>Feed notifications are accessed in an <i>iterator</i> style.
     *
     */
    public Changes continuousChanges() {
        final URI uri = this.databaseHelper.changesUri("feed", "continuous");
        final InputStream in = dbc.couchDbClient.get(uri);
        try {
            final InputStreamReader is = new InputStreamReader(in, "UTF-8");
            setReader(new BufferedReader(is));
        } catch (UnsupportedEncodingException e) {
            // This should never happen as every implementation of the java platform is required
            // to support UTF-8.
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * Checks whether a feed is available in the continuous stream, blocking
     * until a feed is received.
     */
    public boolean hasNext() {
        return readNextRow();
    }

    /**
     * @return The next feed in the stream.
     */
    public Row next() {
        return getNextRow();
    }

    /**
     * Stops a running continuous feed.
     */
    public void stop() {
        stop = true;
    }

    /**
     * Requests Change notifications of feed type normal.
     */
    public ChangesResult getChanges() {
        final URI uri = this.databaseHelper.changesUri("feed", "normal");
        return dbc.couchDbClient.get(uri, ChangesResult.class);
    }

    // Query Params

    public Changes since(String since) {
        this.databaseHelper.query("since", since);
        return this;
    }

    public Changes limit(int limit) {
        this.databaseHelper.query("limit", limit);
        return this;
    }

    public Changes heartBeat(long heartBeat) {
        this.databaseHelper.query("heartbeat", heartBeat);
        return this;
    }

    public Changes timeout(long timeout) {
        this.databaseHelper.query("timeout", timeout);
        return this;
    }

    public Changes filter(String filter) {
        this.databaseHelper.query("filter", filter);
        return this;
    }

    public Changes includeDocs(boolean includeDocs) {
        this.databaseHelper.query("include_docs", includeDocs);
        return this;
    }

    public Changes style(String style) {
        this.databaseHelper.query("style", style);
        return this;
    }

    // Helper

    /**
     * Reads and sets the next feed in the stream.
     */
    private boolean readNextRow() {
        boolean hasNext = false;
        try {
            if (!stop) {
                while (true) {
                    String row = getReader().readLine();
                    if (row.isEmpty()) {
                        continue;
                    }
                    if (row != null && !row.startsWith("{\"last_seq\":")) {
                        setNextRow(gson.fromJson(row, Row.class));
                        hasNext = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            terminate();
            throw new CouchDbException("Error reading continuous stream.", e);
        }
        if (!hasNext) {
            terminate();
        }
        return hasNext;
    }

    private BufferedReader getReader() {
        return reader;
    }

    private void setReader(BufferedReader reader) {
        this.reader = reader;
    }

    private Row getNextRow() {
        return nextRow;
    }

    private void setNextRow(Row nextRow) {
        this.nextRow = nextRow;
    }

    private void terminate() {
        CouchDbUtil.close(getReader());
    }
}
