/*
 * Copyright (C) 2011 lightcouch.org
 * Copyright © 2015, 2017 IBM Corp. All rights reserved.
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

package com.cloudant.client.api;


import com.cloudant.client.api.model.ChangesResult;
import com.cloudant.client.api.model.ChangesResult.Row;
import com.cloudant.client.internal.DatabaseURIHelper;
import com.cloudant.client.org.lightcouch.CouchDbClient;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.internal.CouchDbUtil;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;

/**
 * <p>Contains the Change Notifications API, supports <i>normal</i> and <i>continuous</i> feed
 * Changes.
 * <p>Usage example for normal feed limiting to 10 results, with a filter:</p>
 * <pre>
 * {@code
 * // feed type normal
 * String since = db.info().getUpdateSeq(); // latest update seq
 * ChangesResult changeResult = db.changes()
 * 	.since(since)
 * 	.limit(10)
 * 	.filter("example/filter")
 * 	.getChanges();
 *
 * //process the ChangesResult
 * for (ChangesResult.Row row : changeResult.getResults()) {
 *   String docId = row.getId()
 *   JsonObject doc = row.getDoc();
 * }
 * }
 * </pre>
 * <P>Usage example for continuous feed, including document content:</P>
 * <pre>
 * {@code
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
 * }
 *
 * //while loop blocks; stop from another thread
 * changes.stop(); // stop continuous feed
 * }
 * </pre>
 *
 * @author Ganesh K Choudhary
 * @see ChangesResult
 * @see <a href="https://console.bluemix.net/docs/services/Cloudant/api/database.html#get-changes"
 * target="_blank">Databases - get changes</a>
 * @since 0.0.1
 */
public class Changes {

    private final CouchDbClient client;
    private final Gson gson;
    private final DatabaseURIHelper databaseHelper;

    private BufferedReader reader;
    private ChangesResult.Row nextRow;
    private boolean stop;

    Changes(CloudantClient client, Database database) {
        this.client = client.couchDbClient;
        this.gson = database.getGson();
        this.databaseHelper = new DatabaseURIHelper(database.getDBUri());
    }

    /**
     * Requests Change notifications of feed type continuous.
     * <p>Feed notifications are accessed in an <i>iterator</i> style.</p>
     * <P>
     * This method will connect to the changes feed; any configuration options applied after calling
     * it will be ignored.
     * </P>
     *
     * @return this Changes instance connected to a continuous feed, use
     * {@link #hasNext()} and {@link #next()} to iterate the changes.
     */
    public Changes continuousChanges() {
        final URI uri = this.databaseHelper.changesUri("feed", "continuous");
        final InputStream in = client.get(uri);
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
     *
     * @return true if there is a change
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
     *
     * @return {@link ChangesResult} encapsulating the normal feed changes
     */
    public ChangesResult getChanges() {
        final URI uri = this.databaseHelper.changesUri("feed", "normal");
        return client.get(uri, ChangesResult.class);
    }

    // Query Params

    /**
     * Return only changes after the specified sequence identifier.
     *
     * @param since sequence identifier or {@code "now"}
     * @return this Changes instance
     */
    public Changes since(String since) {
        this.databaseHelper.query("since", since);
        return this;
    }

    /**
     * Limit the number of rows to return.
     *
     * @param limit the number of rows
     * @return this Changes instance
     */
    public Changes limit(int limit) {
        this.databaseHelper.query("limit", limit);
        return this;
    }

    /**
     * Enable an empty line heartbeat for longpoll or continuous feeds for when there have been no
     * changes.
     *
     * @param heartBeat time in milliseconds after which an empty line is sent
     * @return this Changes instance
     */
    public Changes heartBeat(long heartBeat) {
        this.databaseHelper.query("heartbeat", heartBeat);
        return this;
    }

    /**
     * Configure a timeout for the changes feed.
     *
     * @param timeout time in milliseconds to wait for data
     * @return this Changes instance
     */
    public Changes timeout(long timeout) {
        this.databaseHelper.query("timeout", timeout);
        return this;
    }

    /**
     * Specify a filter function to apply to the changes feed.
     *
     * @param filter name of the design document filter function e.g {@code
     * "designDoc/filterFunction"}
     * @return this Changes instance
     */
    public Changes filter(String filter) {
        this.databaseHelper.query("filter", filter);
        return this;
    }

    /**
     * @param includeDocs whether to include document content in the returned rows
     * @return this Changes instance
     */
    public Changes includeDocs(boolean includeDocs) {
        this.databaseHelper.query("include_docs", includeDocs);
        return this;
    }

    /**
     * Configures how many changes are returned "main_only" for the winning revision only or
     * "all_docs" to also include leaf revisions.
     *
     * @param style {@code "main_only"} or {@code "all_docs"}
     * @return this Changes instance
     */
    public Changes style(String style) {
        this.databaseHelper.query("style", style);
        return this;
    }

    /**
     * @param descending {@code true} to return changes in descending order
     * @return this Changes instance
     * @since 2.5.0
     */
    public Changes descending(boolean descending) {
        this.databaseHelper.query("descending", descending);
        return this;
    }

    /**
     * Add a custom query parameter to the _changes request. Useful for specifying extra parameters
     * to a filter function for example.
     *
     * @param name  the name of the query parameter
     * @param value the value of the query parameter
     * @return this Changes instance
     * @since 2.5.0
     */
    public Changes parameter(String name, String value) {
        this.databaseHelper.query(name, value);
        return this;
    }

    // Helper

    /**
     * Reads and sets the next feed in the stream.
     */
    private boolean readNextRow() {
        while (!stop) {
            String row = getLineWrapped();
            // end of stream - null indicates end of stream before we see last_seq which shouldn't
            // be possible but we should handle it
            if (row == null || row.startsWith("{\"last_seq\":")) {
                terminate();
                return false;
            } else if (row.isEmpty()) {
                // heartbeat
                continue;
            }
            setNextRow(gson.fromJson(row, ChangesResult.Row.class));
            return true;
        }
        // we were stopped, end of changes feed
        terminate();
        return false;
    }

    private String getLineWrapped() {
        try {
            return getReader().readLine();
        } catch (IOException ioe) {
            terminate();
            throw new CouchDbException("Error reading continuous stream.", ioe);
        }
    }

    private BufferedReader getReader() {
        return reader;
    }

    private void setReader(BufferedReader reader) {
        this.reader = reader;
    }

    private ChangesResult.Row getNextRow() {
        return nextRow;
    }

    private void setNextRow(ChangesResult.Row nextRow) {
        this.nextRow = nextRow;
    }

    private void terminate() {
        CouchDbUtil.close(getReader());
    }
}
