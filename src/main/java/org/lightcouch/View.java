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

package org.lightcouch;

import static java.lang.String.format;
import static org.lightcouch.internal.CouchDbUtil.JsonToObject;
import static org.lightcouch.internal.CouchDbUtil.assertNotEmpty;
import static org.lightcouch.internal.CouchDbUtil.close;
import static org.lightcouch.internal.CouchDbUtil.getAsLong;
import static org.lightcouch.internal.CouchDbUtil.getStream;
import static org.lightcouch.internal.CouchDbUtil.listResources;
import static org.lightcouch.internal.CouchDbUtil.readFile;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lightcouch.DesignDocument.MapReduce;
import org.lightcouch.internal.URIBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class provides access to the <tt>View</tt> APIs.
 * <p/>
 * <h3>Usage Example:</h3>
 * <pre>
 * {@code
 *  List<Foo> list = db.view("example/foo")
 * 	.startKey("start-key")
 * 	.endKey("end-key")
 * 	.limit(10)
 * 	.includeDocs(true)
 * 	.query(Foo.class);
 *
 *  // scalar values
 *  int count = db.view("example/by_tag")
 * 	.key("couchdb")
 * 	.queryForInt();
 *
 * // pagination
 * Page<Foo> page = db.view("example/foo").queryPage(...);
 * }
 * </pre>
 *
 * @author Ahmed Yehia
 * @see CouchDatabaseBase#view(String)
 * @see ViewResult
 * @since 0.0.2
 */
public class View {
    private static final Log log = LogFactory.getLog(CouchDbClient.class);

    // temp views
    private static final String TEMP_VIEWS_DIR = "temp-views";
    private static final String MAP_JS = "map.js";
    private static final String REDUCE_JS = "reduce.js";

    // view fields
    private JsonElement key;
    private JsonElement startKey;
    private String startKeyDocId;
    private JsonElement endKey;
    private String endKeyDocId;
    private Integer limit;
    private String stale;
    private Boolean descending = false;
    private Integer skip;
    private Boolean group;
    private Integer groupLevel;
    private Boolean reduce;
    private Boolean includeDocs;
    private Boolean inclusiveEnd;
    private Boolean updateSeq;

    private CouchDatabaseBase dbc;
    private Gson gson;
    private URIBuilder uriBuilder;

    private String allDocsKeys; // bulk docs
    private MapReduce mapRedtempViewM; // temp view

    View(CouchDatabaseBase dbc, String viewId) {
        assertNotEmpty(viewId, "View id");
        this.dbc = dbc;
        this.gson = dbc.getGson();

        String view = viewId;
        if (viewId.contains("/")) {
            String[] v = viewId.split("/");
            view = String.format("_design/%s/_view/%s", v[0], v[1]);
        }
        this.uriBuilder = URIBuilder.buildUri(dbc.getDBUri()).path(view);
    }

    // Query options

    /**
     * Queries a view as an {@link InputStream}
     * <p>The stream should be properly closed after usage, as to avoid connection leaks.
     *
     * @return The result as an {@link InputStream}.
     */
    public InputStream queryForStream() {
        URI uri = uriBuilder.build();
        if (allDocsKeys != null) { // bulk docs
            return getStream(dbc.post(uri, allDocsKeys));
        }
        if (mapRedtempViewM != null) { // temp view
            return getStream(dbc.post(uri, gson.toJson(mapRedtempViewM)));
        }

        return dbc.get(uri);
    }

    /**
     * Queries a view.
     *
     * @param <T>      Object type T
     * @param classOfT The class of type T
     * @return The result of the view query as a {@code List<T> }
     */
    public <T> List<T> query(Class<T> classOfT) {
        InputStream instream = null;
        try {
            Reader reader = new InputStreamReader(instream = queryForStream(), "UTF-8");
            JsonArray jsonArray = new JsonParser().parse(reader)
                    .getAsJsonObject().getAsJsonArray("rows");
            List<T> list = new ArrayList<T>();
            for (JsonElement jsonElem : jsonArray) {
                JsonElement elem = jsonElem.getAsJsonObject();
                if (Boolean.TRUE.equals(this.includeDocs)) {
                    elem = jsonElem.getAsJsonObject().get("doc");
                }
                T t = this.gson.fromJson(elem, classOfT);
                list.add(t);
            }
            return list;
        } catch (UnsupportedEncodingException e) {
            // This should never happen as every implementation of the java platform is required
            // to support UTF-8.
            throw new RuntimeException(e);
        } finally {
            close(instream);
        }
    }

    /**
     * Queries a view.
     *
     * @param <K>      Object type K (key)
     * @param <V>      Object type V (value)
     * @param classOfK The class of type K.
     * @param classOfV The class of type V.
     * @param classOfT The class of type T.
     * @return The View result entries.
     */
    public <K, V, T> ViewResult<K, V, T> queryView(Class<K> classOfK, Class<V> classOfV, Class<T>
            classOfT) {
        InputStream instream = null;
        try {
            Reader reader = new InputStreamReader(instream = queryForStream(), "UTF-8");
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            ViewResult<K, V, T> vr = new ViewResult<K, V, T>();
            vr.setTotalRows(getAsLong(json, "total_rows"));
            vr.setUpdateSeq(getAsLong(json, "update_seq"));
            JsonArray jsonArray = json.getAsJsonArray("rows");
            if (jsonArray.size() == 0) { // validate available rows
                throw new NoDocumentException("No result was returned by this view query.");
            }
            for (JsonElement e : jsonArray) {
                ViewResult<K, V, T>.Rows row = vr.new Rows();
                row.setId(JsonToObject(gson, e, "id", String.class));
                row.setKey(JsonToObject(gson, e, "key", classOfK));
                row.setValue(JsonToObject(gson, e, "value", classOfV));
                if (Boolean.TRUE.equals(this.includeDocs)) {
                    row.setDoc(JsonToObject(gson, e, "doc", classOfT));
                }
                vr.getRows().add(row);
            }
            return vr;
        } catch (UnsupportedEncodingException e1) {
            // This should never happen as every implementation of the java platform is required
            // to support UTF-8.
            throw new RuntimeException(e1);
        } finally {
            close(instream);
        }
    }

    /**
     * @return The result of the view as String.
     */
    public String queryForString() {
        return queryValue(String.class);
    }

    /**
     * @return The result of the view as int.
     */
    public int queryForInt() {
        return queryValue(int.class);
    }

    /**
     * @return The result of the view as long.
     */
    public long queryForLong() {
        return queryValue(long.class);
    }

    /**
     * @return The result of the view as boolean.
     */
    public boolean queryForBoolean() {
        return queryValue(boolean.class);
    }

    /**
     * Queries for scalar values. Internal use.
     */
    private <V> V queryValue(Class<V> classOfV) {
        InputStream instream = null;
        try {
            Reader reader = new InputStreamReader(instream = queryForStream(), "UTF-8");
            JsonArray array = new JsonParser().parse(reader).
                    getAsJsonObject().get("rows").getAsJsonArray();
            if (array.size() != 1) { // expect exactly 1 row
                throw new NoDocumentException("Expecting exactly a single result of this view " +
                        "query, but was: " + array.size());
            }
            return JsonToObject(gson, array.get(0), "value", classOfV);
        } catch (UnsupportedEncodingException e) {
            // This should never happen as every implementation of the java platform is required
            // to support UTF-8.
            throw new RuntimeException(e);
        } finally {
            close(instream);
        }
    }

    /**
     * Queries a view for pagination, returns a next or a previous page, this method
     * figures out which page to return based on the given param that is generated by an
     * earlier call to this method, quering the first page is done by passing a {@code null} param.
     *
     * @param <T>         Object type T
     * @param rowsPerPage The number of rows per page.
     * @param param       The request parameter to use to query a page, or {@code null} to return
     *                    the first page.
     * @param classOfT    The class of type T.
     * @return {@link Page}
     */
    public <T> Page<T> queryPage(int rowsPerPage, String param, Class<T> classOfT) {
        // set view query params
        //we want to retrieve the number of required rows, plus 1 additional to determine the start
        //key for the next page if there is one
        limit(rowsPerPage + 1);
        includeDocs(true);

        int thisPageNumber;
        PageMetadata pageToRetrieveMetadata = null;
        if (param != null) {
            pageToRetrieveMetadata = PageMetadata.decode(gson, param);
            //set up the parameters from the supplied pageToRetrieveMetadata link
            startKey(pageToRetrieveMetadata.startKey);
            startKeyDocId(pageToRetrieveMetadata.startKeyDocId);
            thisPageNumber = pageToRetrieveMetadata.pageNumber;
        } else {
            //null param implies first page
            thisPageNumber = 1;
            //we can only page forward from the first page
            pageToRetrieveMetadata = new PageMetadata(PageMetadata.PagingDirection.FORWARD);
        }

        //pages only have a reference to the start key, when paging backwards this is the startkey
        // of the following page (i.e. the last element of the previous page) so to correctly
        // present page results when paging backwards requires a _temporary_ reversal
        if (PageMetadata.PagingDirection.BACKWARD == pageToRetrieveMetadata.direction) {
            //reverse whichever direction this view is normally doing
            uriBuilder.query("descending", !descending);
        }

        // init page, results list
        final Page<T> page = new Page<T>();
        final List<T> resultList = new ArrayList<T>();

        List<ViewResult<JsonElement, JsonElement, T>.Rows> rows = Collections.emptyList();
        int resultRows = 0;
        Long totalRows = 0l;
        try {
            //query the view
            final ViewResult<JsonElement, JsonElement, T> vr = queryView(JsonElement.class,
                    JsonElement.class, classOfT);

            rows = vr.getRows();
            resultRows = rows.size();
            totalRows = vr.getTotalRows();

            if (PageMetadata.PagingDirection.BACKWARD == pageToRetrieveMetadata.direction) {
                //Result needs reversing because to implement backward paging the view reading
                // order is reversed
                Collections.reverse(rows);
            }
        } finally {
            //ensure the view descending parameter is assigned back to its previous value
            uriBuilder.query("descending", descending);
        }

        //we expect limit = rowsPerPage + 1 results, if we have rowsPerPage or less we are on the
        // last page
        boolean isLastPage = resultRows <= rowsPerPage;

        //Loop through the view results, except the last row populating the result list
        for (int i = 0; i < resultRows - 1; i++) {
            // add the element to the result list
            resultList.add(rows.get(i).getDoc());
        }

        // If we are not on the last page, we need to use the last
        // result as the start key for the next page and therefore
        // we don't return it to the user.
        // If we are on the last page, the final row should be returned
        // to the user.
        int lastIndex = resultRows - 1;
        if (!isLastPage) {
            //not the last page, so there is a next page
            page.setHasNext(true);
            //Construct the next page metadata (i.e. paging forward)
            PageMetadata nextPageMetadata = new PageMetadata(PageMetadata.PagingDirection
                    .FORWARD);
            //the last element is the start of the next page
            nextPageMetadata.startKey = rows.get(lastIndex).getKey();
            nextPageMetadata.startKeyDocId = rows.get(lastIndex).getId();
            //increment the page number for the next page
            nextPageMetadata.pageNumber = thisPageNumber + 1;
            //set the parameter
            page.setNextParam(PageMetadata.encode(gson, nextPageMetadata));
        } else {
            // last page
            page.setHasNext(false);
            //add the final row
            resultList.add(rows.get(lastIndex).getDoc());
        }

        //set previous page links if not the first page
        if (thisPageNumber == 1) {
            page.setHasPrevious(false);
        } else {
            page.setHasPrevious(true);
            //set up previous page data, i.e. paging backward
            PageMetadata previousPageMetadata = new PageMetadata(PageMetadata.PagingDirection
                    .BACKWARD);
            //decrement the page number for the previous page
            previousPageMetadata.pageNumber = thisPageNumber - 1;
            //this page's startKey will also be the startKey for the previous page, but with a
            // descending lookup indicated by the paging direction
            previousPageMetadata.startKey = rows.get(0).getKey();
            previousPageMetadata.startKeyDocId = rows.get(0).getId();
            //set the parameter
            page.setPreviousParam(PageMetadata.encode(gson, previousPageMetadata));
        }

        // calculate paging display info
        page.setResultList(resultList);
        page.setTotalResults(totalRows);
        page.setPageNumber(thisPageNumber);
        int offset = (thisPageNumber - 1) * rowsPerPage;
        //given that totalRows is a long, the indexes for "from" and "to" should be long as well
        //however, the API for org.lightcouch.Page uses int so we have to convert
        //TODO fix this next time we change the API
        int resultFrom = offset + 1;
        int resultTo = offset + (isLastPage ? resultRows : rowsPerPage);
        page.setResultFrom(resultFrom);
        page.setResultTo(resultTo);

        return page;
    }

    // fields

    /**
     * @param key The key value, accepts a single value or multiple values for complex keys.
     */
    public View key(Object... key) {
        this.key = getKeyAsJsonElement(key);
        uriBuilder.query("key", gson.toJson(this.key));
        return this;
    }

    /**
     * @param startKey The start key value, accepts a single value or multiple values for complex
     *                 keys.
     */
    public View startKey(Object... startKey) {
        this.startKey = getKeyAsJsonElement(startKey);
        uriBuilder.query("startkey", gson.toJson(this.startKey));
        return this;
    }

    public View startKeyDocId(String startKeyDocId) {
        this.startKeyDocId = startKeyDocId;
        uriBuilder.query("startkey_docid", this.startKeyDocId);
        return this;
    }

    /**
     * @param endKey The end key value, accepts a single value or multiple values for complex keys.
     */
    public View endKey(Object... endKey) {
        this.endKey = getKeyAsJsonElement(endKey);
        uriBuilder.query("endkey", gson.toJson(this.endKey));
        return this;
    }

    public View endKeyDocId(String endKeyDocId) {
        this.endKeyDocId = endKeyDocId;
        uriBuilder.query("endkey_docid", this.endKeyDocId);
        return this;
    }

    public View limit(Integer limit) {
        this.limit = limit;
        uriBuilder.query("limit", this.limit);
        return this;
    }

    /**
     * @param stale Accept values: ok | update_after (update_after as of CouchDB 1.1.0)
     */
    public View stale(String stale) {
        this.stale = stale;
        uriBuilder.query("stale", this.stale);
        return this;
    }

    /**
     * Reverses the reading direction, not the sort order.
     */
    public View descending(Boolean descending) {
        this.descending = descending;
        uriBuilder.query("descending", this.descending);
        return this;
    }

    /**
     * @param skip Skips <i>n</i> number of documents.
     */
    public View skip(Integer skip) {
        this.skip = skip;
        uriBuilder.query("skip", this.skip);
        return this;
    }

    /**
     * @param group Specifies whether the reduce function reduces the result to a set of keys,
     *              or to a single result. Defaults to false (single result).
     */
    public View group(Boolean group) {
        this.group = group;
        uriBuilder.query("group", this.group);
        return this;
    }

    public View groupLevel(Integer groupLevel) {
        this.groupLevel = groupLevel;
        uriBuilder.query("group_level", this.groupLevel);
        return this;
    }

    /**
     * @param reduce Indicates whether to use the reduce function of the view,
     *               defaults to true if the reduce function is defined.
     */
    public View reduce(Boolean reduce) {
        this.reduce = reduce;
        uriBuilder.query("reduce", this.reduce);
        return this;
    }

    public View includeDocs(Boolean includeDocs) {
        this.includeDocs = includeDocs;
        uriBuilder.query("include_docs", this.includeDocs);
        return this;
    }

    /**
     * @param inclusiveEnd Indicates whether the endkey is included in the result,
     *                     defaults to true.
     */
    public View inclusiveEnd(Boolean inclusiveEnd) {
        this.inclusiveEnd = inclusiveEnd;
        uriBuilder.query("inclusive_end", this.inclusiveEnd);
        return this;
    }

    public View updateSeq(Boolean updateSeq) {
        this.updateSeq = updateSeq;
        uriBuilder.query("update_seq", this.updateSeq);
        return this;
    }

    /**
     * Supplies a key list when calling <tt>_all_docs</tt> View.
     *
     * @param keys
     * @return
     */
    public View keys(Object... keys) {
        JsonObject keysObject = new JsonObject();
        keysObject.add("keys", getKeyAsJsonElement(keys));
        this.allDocsKeys = gson.toJson(keysObject);
        return this;
    }

    // temp views

    public View tempView(String id) {
        assertNotEmpty(id, "id");
        String viewPath = format("%s/%s/", TEMP_VIEWS_DIR, id);
        List<String> dirList = listResources(viewPath);
        assertNotEmpty(dirList, "Temp view directory");

        mapRedtempViewM = new MapReduce();
        for (String mapRed : dirList) {
            String def = readFile(format("/%s%s", viewPath, mapRed));
            if (MAP_JS.equals(mapRed)) {
                mapRedtempViewM.setMap(def);
            } else if (REDUCE_JS.equals(mapRed)) {
                mapRedtempViewM.setReduce(def);
            }
        }
        return this;
    }

    public View tempView(MapReduce mapReduce) {
        assertNotEmpty(mapReduce, "mapReduce");
        mapRedtempViewM = mapReduce;
        return this;
    }

    //utilities

    private JsonElement getKeyAsJsonElement(Object... key) {
        // single or complex key
        if (key.length == 1) {
            return gson.toJsonTree(key[0]);
        } else {
            return gson.toJsonTree(key).getAsJsonArray();
        }
    }

    /**
     * <P>
     * Object for serializing metadata about a page.
     * </P>
     * <P>
     * Each call to {@link View#queryPage(int, String, Class)} calculates the parameters required
     * to get the pages preceding and following it. This class is used to persist the values for
     * either page as tokens a caller can pass back to queryPage to retrieve them at a later point.
     * </P>
     */
    static final class PageMetadata {
        /**
         * The start key of the page to retrieve
         */
        @SerializedName("sk")
        JsonElement startKey;
        /**
         * The start key docId of the page to retrieve
         */
        @SerializedName("sd")
        String startKeyDocId;
        /**
         * The page number of the page to retrieve
         */
        @SerializedName("p")
        int pageNumber;
        /**
         * Indicate the paging direction for this metadata
         */
        @SerializedName("d")
        private PagingDirection direction;

        enum PagingDirection {
            @SerializedName("f")
            FORWARD,
            @SerializedName("b")
            BACKWARD
        }

        PageMetadata(PagingDirection direction) {
            this.direction = direction;
        }

        /**
         * @param gson
         * @param metadata
         * @return Base64 encoded string of the GSON serialized metadata object
         */
        static String encode(Gson gson, PageMetadata metadata) {
            if (metadata == null) {
                return null;
            }
            String jsonMetadata = gson.toJson(metadata);
            return Base64.encodeBase64URLSafeString(jsonMetadata
                    .getBytes());
        }

        /**
         * @param gson
         * @param pageParameter
         * @return metadata object deserialized from the Base64 encoded parameter
         */
        static PageMetadata decode(Gson gson, String pageParameter) {
            if (pageParameter == null) {
                return null;
            }
            try {
                // extract fields from the returned HEXed JSON object
                String jsonMetadata = new String(Base64.decodeBase64(pageParameter
                        .getBytes()));
                if (log.isDebugEnabled()) {
                    log.debug("Paging Param Decoded = " + jsonMetadata);
                }
                return gson.fromJson(jsonMetadata, PageMetadata.class);
            } catch (Exception e) {
                throw new CouchDbException("could not parse the given param!", e);
            }
        }
    }
}
