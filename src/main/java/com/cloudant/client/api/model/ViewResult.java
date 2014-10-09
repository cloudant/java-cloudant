package com.cloudant.client.api.model;

import java.util.ArrayList;
import java.util.List;

import org.lightcouch.View;
/**
 * Holds a view result entries. 
 * @since 0.0.1
 * @see View
 * @author Ganesh K Choudhary
 */
public class ViewResult<K, V, T> {
	private org.lightcouch.ViewResult<K, V, T> viewResult ;
	
	public ViewResult(){
		viewResult = new org.lightcouch.ViewResult<>();
	}
	
	public ViewResult(org.lightcouch.ViewResult<K, V, T> viewResult){
		this.viewResult = viewResult ;
	}

	/**
	 * @param obj
	 * @return
	 */
	public boolean equals(Object obj) {
		return viewResult.equals(obj);
	}

	/**
	 * @return
	 */
	public long getTotalRows() {
		return viewResult.getTotalRows();
	}

	/**
	 * @return
	 */
	public long getUpdateSeq() {
		return viewResult.getUpdateSeq();
	}

	/**
	 * @return
	 */
	public int getOffset() {
		return viewResult.getOffset();
	}

	/**
	 * @return
	 */
	public List<Rows> getRows() {
		List<Rows> rows = new ArrayList<Rows>();
		List<org.lightcouch.ViewResult<K,V,T>.Rows> couchDbRows = viewResult.getRows();
		for(int i = 0 ; i < couchDbRows.size(); i++){
			org.lightcouch.ViewResult<K,V,T>.Rows couchDbRow = couchDbRows.get(i);
			Rows row = new Rows(couchDbRow);
			rows.add(row);
		}
		return rows ;
	}

	/**
	 * @param totalRows
	 */
	public void setTotalRows(long totalRows) {
		viewResult.setTotalRows(totalRows);
	}

	/**
	 * @param updateSeq
	 */
	public void setUpdateSeq(long updateSeq) {
		viewResult.setUpdateSeq(updateSeq);
	}

	/**
	 * @param offset
	 */
	public void setOffset(int offset) {
		viewResult.setOffset(offset);
	}

	/**
	 * @param rows
	 */
	public void setRows(List<Rows> rows) {
		List<org.lightcouch.ViewResult<K,V,T>.Rows> rowsList = new ArrayList<org.lightcouch.ViewResult<K,V,T>.Rows>();
		for(int i = 0 ; i < rows.size() ; i++){
			Rows row = rows.get(i);
			org.lightcouch.ViewResult<K,V,T>.Rows lightcouchRows = row.getRows().getrows();
			rowsList.add(lightcouchRows);
		}
		viewResult.setRows(rowsList);
	}

	/**
	 * @return
	 */
	public String toString() {
		return viewResult.toString();
	}
	
	public class Rows {
		private org.lightcouch.ViewResult<K,V,T>.Rows rows ;
		
		
		public Rows(){
			rows = viewResult.new Rows();
		}
		
		Rows(org.lightcouch.ViewResult<K,V,T>.Rows rows){
			this.rows = rows ;
		}
		
		/**
		 * @return the rows
		 */
		public ViewResult<K, V, T>.Rows getRows() {
			return this;
		}

		/**
		 * @return
		 */
		public String getId() {
			return rows.getId();
		}

		/**
		 * @return
		 */
		public K getKey() {
			return rows.getKey();
		}

		/**
		 * @return
		 */
		public V getValue() {
			return rows.getValue();
		}

		/**
		 * @return
		 */
		public T getDoc() {
			return rows.getDoc();
		}


		/**
		 * @param id
		 */
		public void setId(String id) {
			rows.setId(id);
		}

		/**
		 * @param key
		 */
		public void setKey(K key) {
			rows.setKey(key);
		}

		/**
		 * @param value
		 */
		public void setValue(V value) {
			rows.setValue(value);
		}

		/**
		 * @param doc
		 */
		public void setDoc(T doc) {
			rows.setDoc(doc);
		}

		/**
		 * @return
		 */
		public String toString() {
			return rows.toString();
		}
		
		org.lightcouch.ViewResult<K,V,T>.Rows getrows() {
			return rows ;

		}
		
	}
	
}
