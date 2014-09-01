package com.cloudant;

public class IndexField {
	
	public enum SortOrder {
		asc,
		desc
	}
	
	private String name;
	private SortOrder order;
	
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the order
	 */
	public SortOrder getOrder() {
		return order;
	}
	
	
	/**
	 * Represents a Cloudant Sort Syntax for a json field. Used to specify
	 * an element of the 'index.fields' array (POST db/_index) and 'sort' array (db/_find) @see <a href = "http://docs.cloudant.com/api/cloudant-query.html#cloudant-query-sort-syntax"> sort Syntax</a>  
	 * @param name can be any field (dotted notation is available for sub-document fields)
	 * @param order can be "asc" or "desc"
	 */
	public IndexField(String name, SortOrder order) {
		this.name = name;
		this.order = order;
	}
	
	public String toString() {
		return name + " : " + order ;
	}
	
}
