package com.cloudant.client.api;

import static org.lightcouch.internal.CouchDbUtil.JsonToObject;
import static org.lightcouch.internal.CouchDbUtil.assertNotEmpty;
import static org.lightcouch.internal.CouchDbUtil.close;
import static org.lightcouch.internal.CouchDbUtil.getAsLong;
import static org.lightcouch.internal.CouchDbUtil.getAsString;
import static org.lightcouch.internal.CouchDbUtil.getStream;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.lightcouch.internal.URIBuilder;

import com.cloudant.client.api.model.SearchResult;
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
 *  List<Bird> birds = db.search("views101/animals")
 *	.limit(10)
 *	.includeDocs(true)
 *	.query("class:bird", Bird.class);
 *  
 * // groups
 * Map<String,List<Bird>> birdGroups = db.search("views101/animals")
 *	.limit(10)
 *	.includeDocs(true)
 *	.queryGroups("class:bird", Bird.class);
 * for ( Entry<String, List<Bird>> group : birdGroups.entrySet()) {
 *		System.out.println("Group Name : " +  group.getKey());
 *		for ( Bird b : group.getValue() ) {
 *			 System.out.println("\t" + b);
 *		 }
 *	}
 *  
 *  // search results object
 *  SearchResult<Bird> result = db.search("views101/animals")
 *   .querySearchResult("class:bird", Bird.class);
 * 
 *  // pagination
 *  SearchResult<Bird> nextPage = db.search("views101/animals")
 *   .bookmark(result.bookmark)
 *   .querySearchResult("class:bird", Bird.class);
 * }
 * </pre>
 * 
 * @see Database#search(String)
 * @see SearchResult
 * @since 0.0.1
 * @author Mario Briggs
 */
public class Search {
	
	private static final Log log = LogFactory.getLog(Search.class);
	
	// search fields
	private Integer limit;
	private boolean includeDocs = false;
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
	 * Queries a Search Index and returns ungrouped results. In case the query
	 *  used grouping, an empty list is returned
	 * @param <T> Object type T
	 * @param query the Lucene query to be passed to the Search index
	 * @param classOfT The class of type T
	 * @return The result of the search query as a {@code List<T> }
	 */
	public <T> List<T> query(String query, Class<T> classOfT) {
		InputStream instream = null;
		List<T> result = new ArrayList<T>();
		try {  
			Reader reader = new InputStreamReader(instream = queryForStream(query), "UTF-8");
			JsonObject json = new JsonParser().parse(reader).getAsJsonObject(); 
			if ( json.has("rows") ) {
				if (!includeDocs) {
					log.warn("includeDocs set to false and attempting to retrieve doc. " +
							"null object will be returned");
				}
				for (JsonElement e : json.getAsJsonArray("rows")) {
					result.add(JsonToObject(db.getGson(), e, "doc", classOfT));
				}
			}
			else {
				log.warn("No ungrouped result available. Use queryGroups() if grouping set");
			}
			return result;
		} catch (UnsupportedEncodingException e1) {
			// This should never happen as every implementation of the java platform is required to support UTF-8.
			throw new RuntimeException(e1);
		}
		finally {
			close(instream);
		}
	}
	
	/**
	 * Queries a Search Index and returns grouped results in a map where key
	 *  of the map is the groupName. In case the query didnt use grouping,
	 *  an empty map is returned
	 * @param <T> Object type T
	 * @param query the Lucene query to be passed to the Search index
	 * @param classOfT The class of type T
	 * @return The result of the grouped search query as a ordered {@code Map<String,T> }
	 */
	public <T> Map<String,List<T>> queryGroups(String query, Class<T> classOfT) {
		InputStream instream = null;
		try {  
			Reader reader = new InputStreamReader(instream = queryForStream(query), "UTF-8");
			JsonObject json = new JsonParser().parse(reader).getAsJsonObject(); 
			Map<String,List<T>> result = new LinkedHashMap<String, List<T>>();
			if ( json.has("groups") ) 	{
				for (JsonElement e : json.getAsJsonArray("groups")) {
					String groupName = e.getAsJsonObject().get("by").getAsString();
					List<T> orows = new ArrayList<T>();
					if (!includeDocs) {
						log.warn("includeDocs set to false and attempting to retrieve doc. " +
								"null object will be returned");
					}
					for (JsonElement rows : e.getAsJsonObject().getAsJsonArray("rows")) {
							orows.add(JsonToObject(db.getGson(), rows, "doc", classOfT));
					}
					result.put(groupName, orows);
				}// end for(groups)
			}// end hasgroups
			else {
				log.warn("No grouped results available. Use query() if non grouped query");
			}
			return result;
		} catch (UnsupportedEncodingException e1) {
			// This should never happen as every implementation of the java platform is required to support UTF-8.
			throw new RuntimeException(e1);
		}
		finally {
			close(instream);
		}
	}
	
	
	/**
	 * Performs a Cloudant Search and returns the result as an {@link SearchResult}
	 * @param <T> Object type T, an instance into which the rows[].doc/group[].rows[].doc
	 *        attribute of the Search result response should be deserialized into. Same
	 *        goes for the rows[].fields/group[].rows[].fields attribute
	 * @param query the Lucene query to be passed to the Search index
	 * @param classOfT The class of type T.
	 * @return The Search result entries
	 */
	public <T> SearchResult<T> querySearchResult(String query, Class<T> classOfT) {
		InputStream instream = null;
		try {  
			Reader reader = new InputStreamReader(instream = queryForStream(query), "UTF-8");
			JsonObject json = new JsonParser().parse(reader).getAsJsonObject(); 
			SearchResult<T> sr = new SearchResult<T>();
			sr.setTotalRows(getAsLong(json, "total_rows")); 
			sr.setBookmark(getAsString(json, "bookmark"));
			if ( json.has("rows") )
			{
				sr.setRows(getRows(json.getAsJsonArray("rows"), sr, classOfT)); 
			}
			else if (json.has("groups")) {
				setGroups(json.getAsJsonArray("groups"), sr, classOfT);
			}
			
			if (json.has("counts") ) {
				sr.setCounts(getFieldsCounts(json.getAsJsonObject("counts").entrySet()));
			}
			
			if (json.has("ranges") ) {
				sr.setRanges(getFieldsCounts(json.getAsJsonObject("ranges").entrySet()));
			}
			return sr;
		} catch (UnsupportedEncodingException e) {
			// This should never happen as every implementation of the java platform is required to support UTF-8.
			throw new RuntimeException(e);
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
			Map<String,Long> ovalues = new HashMap<String, Long>();
			if ( fld.getValue().isJsonObject() ) {
				Set<Map.Entry<String,JsonElement>> values = fld.getValue().getAsJsonObject().entrySet();
				for ( Entry<String,JsonElement> value : values) {
					ovalues.put(value.getKey(), value.getValue().getAsLong());
				}
			}
			map.put(field, ovalues);
		}
		return map;
	}
	
	private <T> List<SearchResult<T>.SearchResultRows> getRows(
					JsonArray jsonrows, SearchResult<T> sr,  Class<T> classOfT) {
		
		List<SearchResult<T>.SearchResultRows> ret = new ArrayList<SearchResult<T>.SearchResultRows>();
		for (JsonElement e : jsonrows) {
			SearchResult<T>.SearchResultRows row = sr.new SearchResultRows();
			JsonObject oe = e.getAsJsonObject();
			row.setId(oe.get("id").getAsString());
			row.setOrder(JsonToObject(db.getGson(), e, "order", Object[].class));
			row.setFields(JsonToObject(db.getGson(), e, "fields", classOfT));
			if (includeDocs) {
				row.setDoc(JsonToObject(db.getGson(), e, "doc", classOfT));
			}
			ret.add(row);
		}
		return ret;
	}
	
	private <T> void setGroups(JsonArray jsongroups, SearchResult<T> sr, Class<T> classOfT) {
		for (JsonElement e : jsongroups) {
			SearchResult<T>.SearchResultGroups group = sr.new SearchResultGroups();
			JsonObject oe = e.getAsJsonObject();
			group.setBy(oe.get("by").getAsString());
			group.setTotalRows(oe.get("total_rows").getAsLong());
			group.setRows(getRows(oe.getAsJsonArray("rows"), sr, classOfT));
			sr.getGroups().add(group);
		}
	}
}

