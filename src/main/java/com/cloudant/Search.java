package com.cloudant;

import static org.lightcouch.internal.CouchDbUtil.JsonToObject;
import static org.lightcouch.internal.CouchDbUtil.assertNotEmpty;
import static org.lightcouch.internal.CouchDbUtil.close;
import static org.lightcouch.internal.CouchDbUtil.getAsLong;
import static org.lightcouch.internal.CouchDbUtil.getAsString;
import static org.lightcouch.internal.CouchDbUtil.getStream;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.client.methods.HttpGet;
import org.lightcouch.internal.URIBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This class provides access to the Cloudant <tt>Search</tt> APIs.
 * 
 * <h3>Usage Example:</h3>
 * <pre>
 * {@code
 *  SearchResult<Fields,Bird> result = db.search("views101/animals")
 *	.limit(10)
 *	.includeDocs(true)
 *	.query("class:bird",Fields.class, Bird.class);
 *  
 * // pagination
 *  SearchResult<Fields,Bird> nextPage = db.search("views101/animals")
 *  .bookmark(result.bookmark)
 *  .query("class:bird",Fields.class, Bird.class);
 * }
 * </pre>
 * 
 * @see Database#search(String)
 * @see SearchResult
 * @since 0.0.1
 * @author Mario Briggs
 */
public class Search {
	
	
	// search fields
	private Integer limit;
	private Integer skip;
	private Boolean includeDocs;
	private String bookmark;
	private Database db;
	private URIBuilder uriBuilder;
	
	
	
	Search(Database db, String searchIndexId) {
		assertNotEmpty(searchIndexId, "searchIndexId");
		this.db = db;
		String search = searchIndexId;
		if(searchIndexId.contains("/")) {
			String[] v = searchIndexId.split("/");
			search = String.format("_design/%s/_search/%s", v[0], v[1]);
		}
		this.uriBuilder = URIBuilder.buildUri(db.getDBUri()).path(search);
	}
	
	// Query options
	
	/**
	 * Performs a Cloudant Search and returns the result as an {@link InputStream}
	 * @param query the Lucene query to be passed to the Search index
	 * <p>The stream should be properly closed after usage, as to avoid connection leaks.
	 * @return The result as an {@link InputStream}.
	 */
	public InputStream queryForStream(String query) {
		key(query);
		URI uri = uriBuilder.build();
		HttpGet get = new HttpGet(uri);
		get.addHeader("Accept", "application/json");
		return getStream(db.executeRequest(get));
	}
	
	/**
	 * Performs a Cloudant Search and returns the result as an {@link SearchResult}
	 * @param <F> Object type F, an instance into which the rows[].fields/group[].rows[].fields
	 *        attribute of the Search result response should be deserialized into. If not
	 *        interested in this attribute, pass any user type  
	 * @param <T> Object type T, an instance into which the rows[].doc/group[].rows[].doc
	 *        attribute of the Search result response should be deserialized into. If not
	 *        interested, do not set includeDocs(true) and pass any object type
	 * @param query the Lucene query to be passed to the Search index
	 * @param classofF The class of type F.
	 * @param classOfT The class of type T.
	 * @return The Search result entries
	 */
	public <F, T> SearchResult<F, T> query(String query, Class<F> classofF, Class<T> classOfT) {
		InputStream instream = null;
		try {  
			Reader reader = new InputStreamReader(instream = queryForStream(query));
			JsonObject json = new JsonParser().parse(reader).getAsJsonObject(); 
			SearchResult<F,T> sr = new SearchResult<F,T>();
			sr.setTotalRows(getAsLong(json, "total_rows")); 
			sr.setBookmark(getAsString(json, "bookmark"));
			if ( json.has("rows") )
			{
				sr.setRows(getRows(json.getAsJsonArray("rows"), sr, classofF, classOfT)); 
			}
			else if (json.has("groups")) {
				setGroups(json.getAsJsonArray("groups"), sr, classofF, classOfT);
			}
			
			if (json.has("counts") ) {
				sr.setCounts(getFieldsCounts(json.getAsJsonObject("counts").entrySet()));
			}
			
			if (json.has("ranges") ) {
				sr.setRanges(getFieldsCounts(json.getAsJsonObject("ranges").entrySet()));
			}
			return sr;
		}
		finally {
			close(instream);
		}
	}
	
	/**
	 * @param limit limit the number of documents in the result
	 */
	public Search limit(Integer limit) {
		this.limit = limit;
		uriBuilder.query("limit", this.limit);
		return this;
	}
	

	/**
	 * Control which page of results to get. The bookmark value is obtained by executing
	 * the query()/queryForStream() once and getting it from the bookmark field 
	 * in the response
	 * @param bookmark see the next page of results after this bookmark result
	 * @return
	 */
	public Search bookmark(String bookmark) {
		this.bookmark = bookmark;
		uriBuilder.query("bookmark", this.bookmark);
		return this;
	}
	
	/**
	 * Specify the sort order for the result.
	 * @param sortJson @see <a href="http://docs.cloudant.com/api/search.html"> sort</a>
	 *  query argument for format
	 * @return
	 */
	public Search sort(String sortJson ) {
		assertNotEmpty(sortJson, "sort");
		uriBuilder.query("sort", sortJson);
		return this;
	}
	
	
	/**
	 * field by which to group results.
	 * @param fieldName
	 * @param isNumber whether field isNumeric.
	 * @return
	 */
	public Search groupField(String fieldName, boolean isNumber) {
		assertNotEmpty(fieldName, "fieldName");
		if (isNumber)
			uriBuilder.query("group_field", fieldName + "<number>");
		else
			uriBuilder.query("group_field", fieldName);
		return this;
	}
	
	/**
	 * Maximum group count when groupField is set
	 * @param limit
	 * @return
	 */
	public Search groupLimit(int limit) {
		uriBuilder.query("group_limit", limit);
		return this;
	}
	
	/**
	 * the sort order of the groups when groupField is set
	 * @param groupsortJson  @see <a href="http://docs.cloudant.com/api/search.html"> sort</a>
	 *  query argument here for format
	 * @return
	 */
	public Search groupSort(String groupsortJson) {
		assertNotEmpty(groupsortJson, "groupsortJson");
		uriBuilder.query("group_sort", groupsortJson);
		return this;
	}
	
	/**
	 * Ranges for faceted searches.  @see <a href="http://docs.cloudant.com/api/search.html"> ranges</a>
	 *  query argument here for format
	 * @param rangesJson
	 * @return
	 */
	public Search ranges(String rangesJson) {
		assertNotEmpty(rangesJson, "rangesJson");
		uriBuilder.query("ranges", rangesJson);
		return this;
	}
	
	/**
	 * Array of fieldNames for which counts should be produced
	 * @param countsfields
	 * @return
	 */
	public Search counts(String[] countsfields) {
		assert(countsfields.length > 0);
		int i = 0;
		String counts = "[";
		for ( ; i < countsfields.length -1; i++ ) {
			counts += "\"" + countsfields[i] + "\",";
		}
		counts += "\"" + countsfields[i] + "\"]";
		uriBuilder.query("counts",  counts );
		return this;
	}
	
	/**
	 *  @see <a href="http://docs.cloudant.com/api/search.html"> drilldown</a>
	 *  query argument 
	 * @param fieldName
	 * @param fieldValue
	 * @return
	 */
	public Search drillDown(String fieldName, String fieldValue) {
		assertNotEmpty(fieldName, "fieldName");
		assertNotEmpty(fieldValue, "fieldValue");
		uriBuilder.query("drilldown", "[\"" + fieldName + "\",\"" + fieldValue + "\"]");
		return this;
	}
	
	
	/**
	 * @param stale Accept values: ok 
	 */
	public Search stale(boolean stale) {
		if ( stale ) {
			uriBuilder.query("stale", "ok");
		}
		return this;
	}
	
	/**
	 * @param skip Skips <i>n</i> number of documents.
	 */
	public Search skip(Integer skip) {
		this.skip = skip;
		uriBuilder.query("skip", this.skip);
		return this;
	}
	
	/**
	 * @param includeDocs whether to include the document in the result
	 */
	public Search includeDocs(Boolean includeDocs) {
		this.includeDocs = includeDocs;
		uriBuilder.query("include_docs", this.includeDocs);
		return this;
	}
	
	
	private void key(String query) {
		uriBuilder.query("q", query);
	}
	
	private Map<String,Map<String,Long>> getFieldsCounts(Set<Map.Entry<String,JsonElement>> fldset) {
		Map<String,Map<String,Long>> map = new HashMap<String, Map<String,Long>>();
		for ( Entry<String,JsonElement> fld : fldset ) {
			String field = fld.getKey();
			Set<Map.Entry<String,JsonElement>> values = fld.getValue().getAsJsonObject().entrySet();
			Map<String,Long> ovalues = new HashMap<String, Long>();
			for ( Entry<String,JsonElement> value : values) {
				ovalues.put(value.getKey(), value.getValue().getAsLong());
			}
			map.put(field, ovalues);
		}
		return map;
	}
	
	private <F, T> List<SearchResult<F,T>.SearchResultRows> getRows(
					JsonArray jsonrows, SearchResult<F, T> sr, Class<F> classOfF, Class<T> classOfT) {
		
		List<SearchResult<F,T>.SearchResultRows> ret = new ArrayList<SearchResult<F,T>.SearchResultRows>();
		for (JsonElement e : jsonrows) {
			SearchResult<F,T>.SearchResultRows row = sr.new SearchResultRows();
			JsonObject oe = e.getAsJsonObject();
			row.setId(oe.get("id").getAsString());
			row.setOrder(JsonToObject(Database.getGson(), e, "order", Number[].class));
			row.setFields(JsonToObject(Database.getGson(), e, "fields", classOfF));
			if (includeDocs) {
				row.setDoc(JsonToObject(Database.getGson(), e, "doc", classOfT));
			}
			ret.add(row);
		}
		return ret;
	}
	
	private <F, T> void setGroups(JsonArray jsongroups, SearchResult<F, T> sr, Class<F> classOfF, Class<T> classOfT) {
		for (JsonElement e : jsongroups) {
			SearchResult<F,T>.SearchResultGroups group = sr.new SearchResultGroups();
			JsonObject oe = e.getAsJsonObject();
			group.setBy(oe.get("by").getAsString());
			group.setTotalRows(oe.get("total_rows").getAsLong());
			group.setRows(getRows(oe.getAsJsonArray("rows"), sr, classOfF, classOfT));
			sr.getGroups().add(group);
		}
	}
}

