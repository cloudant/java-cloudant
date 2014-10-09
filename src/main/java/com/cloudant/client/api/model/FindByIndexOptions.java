package com.cloudant.client.api.model;

import java.util.ArrayList;
import java.util.List;

import com.cloudant.client.api.Database;

/**
 * Options to set on findByIndex() request.
 * <p>Example: 
 * <pre>
 * database.findByIndex(
 * 	   " "selector": { "Movie_year": {"$gt": 1960}, "Person_name": "Alec Guinness" }"
 * 		Movie.class, 
 * 		new FindByIndexOptions()
			.sort(new IndexField("Movie_year", SortOrder.desc))
			.fields("Movie_name").fields("Movie_year")
			.limit(1)
			.skip(1)
			.readQuorum(2));
 * </pre>
 * @see Database#findByIndex(String, Class, FindByIndexOptions)
 * @since 0.0.1
 * @author Mario Briggs
 * 
 */
public class FindByIndexOptions {

	// search fields
	private Integer limit;
	private Integer skip;
	private List<IndexField> sort = new ArrayList<IndexField>();
	private List<String> fields = new ArrayList<String>();
	private Integer readQuorum;
		
	/**
	 * @param limit limit the number of results return
	 */
	public FindByIndexOptions limit(Integer limit) {
		this.limit = limit;
		return this;
	}
	
	/**
	 * @param skip Skips <i>n</i> number of results.
	 */
	public FindByIndexOptions skip(Integer skip) {
		this.skip = skip;
		return this;
	}
	
	/**
	 * @param readQuorum set the readQuorum
	 */
	public FindByIndexOptions readQuorum(Integer readQuorum) {
		this.readQuorum = readQuorum;
		return this;
	}
	
	/**
	 *  Can be called multiple times to set the sort syntax
	 * @param sort add a sort syntax field
	 */
	public FindByIndexOptions sort(IndexField sort) {
		this.sort.add(sort);
		return this;
	}
	
	
	/**
	 * Can be called multiple times to set the list of return fields
	 * @param field set the return fields
	 */
	public FindByIndexOptions fields(String field) {
		this.fields.add(field);
		return this;
	}
	
	public List<String> getFields() {
		return fields;
	}
	
	public List<IndexField> getSort() {
		return sort;
	}
	
	public Integer getLimit() {
		return limit;
	}
	
	public Integer getSkip() {
		return skip;
	}
	
	public Integer getReadQuorum() {
		return readQuorum;
	}
	
}
