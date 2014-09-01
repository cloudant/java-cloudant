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
	
	
	public IndexField(String name, SortOrder order) {
		this.name = name;
		this.order = order;
	}
	
	public String toString() {
		return name + " : " + order ;
	}
	
}
