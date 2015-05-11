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
import static org.lightcouch.internal.CouchDbUtil.getAsInt;
import static org.lightcouch.internal.CouchDbUtil.getAsLong;
import static org.lightcouch.internal.CouchDbUtil.getStream;
import static org.lightcouch.internal.CouchDbUtil.listResources;
import static org.lightcouch.internal.CouchDbUtil.readFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lightcouch.DesignDocument.MapReduce;
import org.lightcouch.internal.URIBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This class provides access to the <tt>View</tt> APIs.
 * 
 * <h3>Usage Example:</h3>
 * <pre>
 * {@code
 *  List<Foo> list = db.view("example/foo")
 *	.startKey("start-key")
 *	.endKey("end-key")
 *	.limit(10)
 *	.includeDocs(true)
 *	.query(Foo.class);
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
 * @see CouchDatabaseBase#view(String)
 * @see ViewResult
 * @since 0.0.2
 * @author Ahmed Yehia
 */
public class View {
	private static final Log log = LogFactory.getLog(CouchDbClient.class);
	
	// paging param fields
	private static final String START_KEY                = "s_k";
	private static final String START_KEY_DOC_ID         = "s_k_d_i";
	private static final String CURRENT_START_KEY        = "c_k";
	private static final String CURRENT_START_KEY_DOC_ID = "c_k_d_i";
	private static final String CURRENT_KEYS             = "c";
	private static final String ACTION                   = "a";
	private static final String NEXT                     = "n";
	private static final String PREVIOUS                 = "p";
	
	// temp views
	private static final String TEMP_VIEWS_DIR           = "temp-views";
	private static final String MAP_JS                   = "map.js";
	private static final String REDUCE_JS                = "reduce.js";
	
	// view fields
	private String key;
	private String startKey;
	private String startKeyDocId;
	private String endKey;
	private String endKeyDocId;
	private Integer limit;
	private String stale;
	private Boolean descending;
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
		if(viewId.contains("/")) {
			String[] v = viewId.split("/");
			view = String.format("_design/%s/_view/%s", v[0], v[1]);
		}
		this.uriBuilder = URIBuilder.buildUri(dbc.getDBUri()).path(view);
	}
	
	// Query options
	
	/**
	 * Queries a view as an {@link InputStream}
	 * <p>The stream should be properly closed after usage, as to avoid connection leaks.
	 * @return The result as an {@link InputStream}.
	 */
	public InputStream queryForStream() {
		URI uri = uriBuilder.build();
		if(allDocsKeys != null) { // bulk docs
			return getStream(dbc.post(uri, allDocsKeys));
		}
		if(mapRedtempViewM != null) { // temp view
			return getStream(dbc.post(uri, gson.toJson(mapRedtempViewM)));
		}
		
		return dbc.get(uri);
	}
	
	/**
	 * Queries a view.
	 * @param <T> Object type T
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
				if(Boolean.TRUE.equals(this.includeDocs)) {
					elem = jsonElem.getAsJsonObject().get("doc");
				}
				T t = this.gson.fromJson(elem, classOfT);
				list.add(t);
			}
			return list;
		} catch (UnsupportedEncodingException e) {
			// This should never happen as every implementation of the java platform is required to support UTF-8.
			throw new RuntimeException(e);
		} finally {
			close(instream);
		}
	}

	/**
	 * Queries a view.
	 * @param <K> Object type K (key)
	 * @param <V> Object type V (value)
	 * @param classOfK The class of type K.
	 * @param classOfV The class of type V.
	 * @param classOfT The class of type T.
	 * @return The View result entries.
	 */
	public <K, V, T> ViewResult<K, V, T> queryView(Class<K> classOfK, Class<V> classOfV, Class<T> classOfT) {
		InputStream instream = null;
		try {  
			Reader reader = new InputStreamReader(instream = queryForStream(), "UTF-8");
			JsonObject json = new JsonParser().parse(reader).getAsJsonObject(); 
			ViewResult<K, V, T> vr = new ViewResult<K, V, T>();
			vr.setTotalRows(getAsLong(json, "total_rows")); 
			vr.setOffset(getAsInt(json, "offset"));
			vr.setUpdateSeq(getAsLong(json, "update_seq"));
			JsonArray jsonArray = json.getAsJsonArray("rows");
			if(jsonArray.size() == 0) { // validate available rows
				throw new NoDocumentException("No result was returned by this view query.");
			}
			for (JsonElement e : jsonArray) {
				ViewResult<K, V, T>.Rows row = vr.new Rows();
				row.setId(JsonToObject(gson, e, "id", String.class));
				row.setKey(JsonToObject(gson, e, "key", classOfK));
				row.setValue(JsonToObject(gson, e, "value", classOfV));
				if(Boolean.TRUE.equals(this.includeDocs)) {
					row.setDoc(JsonToObject(gson, e, "doc", classOfT));
				}
				vr.getRows().add(row);
			}
			return vr;
		} catch (UnsupportedEncodingException e1) {
			// This should never happen as every implementation of the java platform is required to support UTF-8.
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
			if(array.size() != 1) { // expect exactly 1 row
				throw new NoDocumentException("Expecting exactly a single result of this view query, but was: " + array.size());
			}
			return JsonToObject(gson, array.get(0), "value", classOfV);
		} catch (UnsupportedEncodingException e) {
			// This should never happen as every implementation of the java platform is required to support UTF-8.
			throw new RuntimeException(e);
		} finally {
			close(instream);
		}
	}
	
	/**
	 * Queries a view for pagination, returns a next or a previous page, this method
	 * figures out which page to return based on the given param that is generated by an
	 * earlier call to this method, quering the first page is done by passing a {@code null} param.
	 * @param <T> Object type T
	 * @param rowsPerPage The number of rows per page.
	 * @param param The request parameter to use to query a page, or {@code null} to return the first page.
	 * @param classOfT The class of type T.
	 * @return {@link Page}
	 */
	public <T> Page<T> queryPage(int rowsPerPage, String param, Class<T> classOfT) {
		if(param == null) { // assume first page
			return queryNextPage(rowsPerPage, null, null, null, null, classOfT);
		}
		String currentStartKey;
		String currentStartKeyDocId;
		String startKey;
		String startKeyDocId;
		String action;
		try {
			// extract fields from the returned HEXed JSON object
			final JsonObject json = new JsonParser().parse(new String(Base64.decodeBase64(param.getBytes()))).getAsJsonObject();
			if(log.isDebugEnabled()) {
				log.debug("Paging Param Decoded = " + json);
			}
			final JsonObject jsonCurrent = json.getAsJsonObject(CURRENT_KEYS);
			currentStartKey = jsonCurrent.get(CURRENT_START_KEY).getAsString();
			currentStartKeyDocId = jsonCurrent.get(CURRENT_START_KEY_DOC_ID).getAsString();
			startKey = json.get(START_KEY).getAsString();
			startKeyDocId = json.get(START_KEY_DOC_ID).getAsString();
			action = json.get(ACTION).getAsString();
		} catch (Exception e) {
			throw new CouchDbException("could not parse the given param!", e);
		}
		if(PREVIOUS.equals(action)) { // previous
			return queryPreviousPage(rowsPerPage, currentStartKey, currentStartKeyDocId, startKey, startKeyDocId, classOfT);
		} else { // next
			return queryNextPage(rowsPerPage, currentStartKey, currentStartKeyDocId, startKey, startKeyDocId, classOfT);
		}
	}
	
	/**
	 * @return The next page.
	 */
	private <T> Page<T> queryNextPage(int rowsPerPage, String currentStartKey, 
			String currentStartKeyDocId, String startKey, String startKeyDocId, Class<T> classOfT) {
		// set view query params
		limit(rowsPerPage + 1);
		includeDocs(true);
		if(startKey != null) { 
			startKey(startKey);
			startKeyDocId(startKeyDocId);
		}
		// init page, query view
		final Page<T> page = new Page<T>();
		final List<T> pageList = new ArrayList<T>();
		final ViewResult<String, String, T> vr = queryView(String.class, String.class, classOfT);
		final List<ViewResult<String, String, T>.Rows> rows = vr.getRows();
		final int resultRows = rows.size();
		final int offset = vr.getOffset();
		final long totalRows = vr.getTotalRows();
		// holds page params
		final JsonObject currentKeys = new JsonObject();
		final JsonObject jsonNext = new JsonObject();
		final JsonObject jsonPrev = new JsonObject();
		currentKeys.addProperty(CURRENT_START_KEY, rows.get(0).getKey());
		currentKeys.addProperty(CURRENT_START_KEY_DOC_ID, rows.get(0).getId());
		for (int i = 0; i < resultRows; i++) {
			// set keys for the next page
			if (i == resultRows - 1) { // last element (i.e rowsPerPage + 1)
				if(resultRows > rowsPerPage) { // if not last page
					page.setHasNext(true);
					jsonNext.addProperty(START_KEY, rows.get(i).getKey());
					jsonNext.addProperty(START_KEY_DOC_ID, rows.get(i).getId());
					jsonNext.add(CURRENT_KEYS, currentKeys);
					jsonNext.addProperty(ACTION, NEXT); 
					page.setNextParam(Base64.encodeBase64URLSafeString(jsonNext.toString().getBytes()));
					continue; // exclude 
				} 
			}
			pageList.add(rows.get(i).getDoc());
		}
		// set keys for the previous page
		if(offset != 0) { // if not first page
			page.setHasPrevious(true);
			jsonPrev.addProperty(START_KEY, currentStartKey);
			jsonPrev.addProperty(START_KEY_DOC_ID, currentStartKeyDocId);
			jsonPrev.add(CURRENT_KEYS, currentKeys);
			jsonPrev.addProperty(ACTION, PREVIOUS); 
			page.setPreviousParam(Base64.encodeBase64URLSafeString(jsonPrev.toString().getBytes()));
		}
		// calculate paging display info
		page.setResultList(pageList);
		page.setTotalResults(totalRows);
		page.setResultFrom(offset + 1);
		final int resultTo = rowsPerPage > resultRows ? resultRows : rowsPerPage; // fix when rowsPerPage exceeds returned rows
		page.setResultTo(offset + resultTo);
		page.setPageNumber((int) Math.ceil(page.getResultFrom() / Double.valueOf(rowsPerPage)));
		return page;
	}
	
	/**
	 * @return The previous page.
	 */
	private <T> Page<T> queryPreviousPage(int rowsPerPage, String currentStartKey, 
			String currentStartKeyDocId, String startKey, String startKeyDocId, Class<T> classOfT) {
		// set view query params
		limit(rowsPerPage + 1);
		includeDocs(true);
		descending(true); // read backward
		startKey(currentStartKey); 
		startKeyDocId(currentStartKeyDocId); 
		// init page, query view
		final Page<T> page = new Page<T>();
		final List<T> pageList = new ArrayList<T>();
		final ViewResult<String, String, T> vr = queryView(String.class, String.class, classOfT);
		final List<ViewResult<String, String, T>.Rows> rows = vr.getRows();
		final int resultRows = rows.size();
		final int offset = vr.getOffset();
		final long totalRows = vr.getTotalRows();
		Collections.reverse(rows); // fix order
		// holds page params
		final JsonObject currentKeys = new JsonObject();
		final JsonObject jsonNext = new JsonObject();
		final JsonObject jsonPrev = new JsonObject();
		currentKeys.addProperty(CURRENT_START_KEY, rows.get(0).getKey());
		currentKeys.addProperty(CURRENT_START_KEY_DOC_ID, rows.get(0).getId());
		for (int i = 0; i < resultRows; i++) {
			// set keys for the next page
			if (i == resultRows - 1) { // last element (i.e rowsPerPage + 1)
				if(resultRows >= rowsPerPage) { // if not last page
					page.setHasNext(true);
					jsonNext.addProperty(START_KEY, rows.get(i).getKey());
					jsonNext.addProperty(START_KEY_DOC_ID, rows.get(i).getId());
					jsonNext.add(CURRENT_KEYS, currentKeys);
					jsonNext.addProperty(ACTION, NEXT); 
					page.setNextParam(Base64.encodeBase64URLSafeString(jsonNext.toString().getBytes()));
					continue; 
				}
			}
			pageList.add(rows.get(i).getDoc());
		}
		// set keys for the previous page
		if(offset != (totalRows - rowsPerPage - 1)) { // if not first page
			page.setHasPrevious(true);
			jsonPrev.addProperty(START_KEY, currentStartKey);
			jsonPrev.addProperty(START_KEY_DOC_ID, currentStartKeyDocId);
			jsonPrev.add(CURRENT_KEYS, currentKeys);
			jsonPrev.addProperty(ACTION, PREVIOUS); 
			page.setPreviousParam(Base64.encodeBase64URLSafeString(jsonPrev.toString().getBytes()));
		}
		// calculate paging display info
		page.setResultList(pageList);
		page.setTotalResults(totalRows);
		page.setResultFrom((int) totalRows - (offset + rowsPerPage));
		final int resultTo = (int) totalRows - offset - 1;
		page.setResultTo(resultTo);
		page.setPageNumber(resultTo / rowsPerPage);
		return page;
	}
	
	// fields
	
	/**
	 * @param key The key value, accepts a single value or multiple values for complex keys.
	 */
	public View key(Object... key) {
		this.key = getKeyAsJson(key);
		uriBuilder.query("key", this.key);
		return this;
	}
	
	/**
	 * @param startKey The start key value, accepts a single value or multiple values for complex keys.
	 */
	public View startKey(Object... startKey) {
		this.startKey = getKeyAsJson(startKey);
		uriBuilder.query("startkey", this.startKey);
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
		this.endKey = getKeyAsJson(endKey);
		uriBuilder.query("endkey", this.endKey);
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
		this.descending = Boolean.valueOf(gson.toJson(descending));
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
	 * or to a single result. Defaults to false (single result).
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
	 * defaults to true if the reduce function is defined.
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
	 * defaults to true.
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
	 * @param keys
	 * @return
	 */
	public View keys(Object... keys) {
		this.allDocsKeys = String.format("{%s:%s}", gson.toJson("keys"), getKeyAsJson(keys));
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
			if(MAP_JS.equals(mapRed))
				mapRedtempViewM.setMap(def);
			else if(REDUCE_JS.equals(mapRed))
				mapRedtempViewM.setReduce(def);
		} 
		return this;
	}
	
	public View tempView(MapReduce mapReduce) {
		assertNotEmpty(mapReduce, "mapReduce");
		mapRedtempViewM = mapReduce;
		return this;
	}
	
	private String getKeyAsJson(Object... key) {
		return (key.length == 1) ? gson.toJson(key[0]) : gson.toJson(key); // single or complex key
	}
}
