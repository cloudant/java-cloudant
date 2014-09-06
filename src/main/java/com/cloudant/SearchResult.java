package com.cloudant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;




import java.util.Map;

import com.google.gson.annotations.SerializedName;

/**
 * Holds a search result entries
 * @author Mario Briggs
 *
 * @param <T> Object type T, an instance into which the rows[].doc/group[].rows[].doc
 *             attribute of the Search result response should be deserialized into.
 *             Same goes for the rows[].fields/group[].rows[].fields attribute
 *             
 */
public class SearchResult<T> {
	
	@SerializedName("total_rows")
	private long totalRows; 
	private String bookmark;
	private List<SearchResultRows> rows = new ArrayList<SearchResultRows>();
	private List<SearchResultGroups> groups = new ArrayList<SearchResultGroups>();
	private Map<String,Map<String,Long>> counts = new HashMap<String, Map<String,Long>>();
	private Map<String,Map<String,Long>> ranges;
	
	
	/**
	 * @return the totalRows
	 */
	public long getTotalRows() {
		return totalRows;
	}

	/**
	 * @param totalRows the totalRows to set
	 */
	public void setTotalRows(long totalRows) {
		this.totalRows = totalRows;
	}

	/**
	 * @return the bookmark
	 */
	public String getBookmark() {
		return bookmark;
	}

	/**
	 * @param bookmark the bookmark to set
	 */
	public void setBookmark(String bookmark) {
		this.bookmark = bookmark;
	}

	
	/**
	 * @return the counts
	 */
	public Map<String, Map<String, Long>> getCounts() {
		return counts;
	}

	

	/**
	 * @param counts the counts to set
	 */
	public void setCounts(Map<String, Map<String, Long>> counts) {
		this.counts = counts;
	}

	/**
	 * @param ranges the ranges to set
	 */
	public void setRanges(Map<String, Map<String, Long>> ranges) {
		this.ranges = ranges;
	}

	/**
	 * @return the ranges
	 */
	public Map<String, Map<String, Long>> getRanges() {
		return ranges;
	}


	

	/**
	 * @return the rows
	 */
	public List<SearchResultRows> getRows() {
		return rows;
	}

	/**
	 * @param rows the rows to set
	 */
	public void setRows(List<SearchResultRows> rows) {
		this.rows = rows;
	}


	/**
	 * @return the groups
	 */
	public List<SearchResultGroups> getGroups() {
		return groups;
	}


	/**
	 * Inner class holding the SearchResult rows.
	 */
	public class SearchResultRows {
		private String id;
		private Object[] order;
		private T fields ;
		private T doc;
		/**
		 * @param id the id to set
		 */
		public void setId(String id) {
			this.id = id;
		}
		/**
		 * @param order the order to set
		 */
		public void setOrder(Object[] order) {
			this.order = order;
		}
		/**
		 * @param fields the fields to set
		 */
		public void setFields(T fields) {
			this.fields = fields;
		}
		/**
		 * @param doc the doc to set
		 */
		public void setDoc(T doc) {
			this.doc = doc;
		}
		/**
		 * @return the id
		 */
		public String getId() {
			return id;
		}
		/**
		 * @return the order (each element can be a String/Number)
		 */
		public Object[] getOrder() {
			return order;
		}
		/**
		 * @return the fields
		 */
		public T getFields() {
			return fields;
		}
		/**
		 * @return the doc
		 */
		public T getDoc() {
			return doc;
		}
		
		
		
	}
	
	/**
	 * Inner class holding the SearchResult Groups.
	 */
	public class SearchResultGroups {
		private String by;
		@SerializedName("total_rows")
		private Long totalRows;
		private List<SearchResultRows> rows = new ArrayList<SearchResultRows>();
		
		
		/**
		 * @return the by
		 */
		public String getBy() {
			return by;
		}
		/**
		 * @param by the by to set
		 */
		public void setBy(String by) {
			this.by = by;
		}
		/**
		 * @return the totalRows
		 */
		public Long getTotalRows() {
			return totalRows;
		}
		/**
		 * @param totalRows the totalRows to set
		 */
		public void setTotalRows(Long totalRows) {
			this.totalRows = totalRows;
		}
		
		/**
		 * @param rows the rows to set
		 */
		public void setRows(List<SearchResultRows> rows) {
			this.rows = rows;
		}
		/**
		 * @return the rows
		 */
		public List<SearchResultRows> getRows() {
			return rows;
		}
		
		
		
	}
	

}
