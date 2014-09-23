package com.cloudant;

import java.util.ArrayList;
import java.util.List;

import org.lightcouch.ViewResult.Rows;

public class ViewResult<K, V, T> {
	private org.lightcouch.ViewResult<K, V, T> viewResult ;
	
	public ViewResult(){
		viewResult = new org.lightcouch.ViewResult<>();
	}
	
	ViewResult(org.lightcouch.ViewResult<K, V, T> viewResult){
		this.viewResult = viewResult ;
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return viewResult.equals(obj);
	}

	/**
	 * @return
	 * @see org.lightcouch.ViewResult#getTotalRows()
	 */
	public long getTotalRows() {
		return viewResult.getTotalRows();
	}

	/**
	 * @return
	 * @see org.lightcouch.ViewResult#getUpdateSeq()
	 */
	public long getUpdateSeq() {
		return viewResult.getUpdateSeq();
	}

	/**
	 * @return
	 * @see org.lightcouch.ViewResult#getOffset()
	 */
	public int getOffset() {
		return viewResult.getOffset();
	}

	/**
	 * @return
	 * @see org.lightcouch.ViewResult#getRows()
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
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return viewResult.hashCode();
	}

	/**
	 * @param totalRows
	 * @see org.lightcouch.ViewResult#setTotalRows(long)
	 */
	public void setTotalRows(long totalRows) {
		viewResult.setTotalRows(totalRows);
	}

	/**
	 * @param updateSeq
	 * @see org.lightcouch.ViewResult#setUpdateSeq(long)
	 */
	public void setUpdateSeq(long updateSeq) {
		viewResult.setUpdateSeq(updateSeq);
	}

	/**
	 * @param offset
	 * @see org.lightcouch.ViewResult#setOffset(int)
	 */
	public void setOffset(int offset) {
		viewResult.setOffset(offset);
	}

	/**
	 * @param rows
	 * @see org.lightcouch.ViewResult#setRows(java.util.List)
	 */
	public void setRows(List<Rows> rows) {
		List<org.lightcouch.ViewResult<K,V,T>.Rows> rowsList = new ArrayList<org.lightcouch.ViewResult<K,V,T>.Rows>();
		for(int i = 0 ; i < rows.size() ; i++){
			Rows row = rows.get(i);
			org.lightcouch.ViewResult<K,V,T>.Rows lightcouchRows = row.getRows();
			rowsList.add(lightcouchRows);
		}
		viewResult.setRows(rowsList);
	}

	/**
	 * @return
	 * @see org.lightcouch.ViewResult#toString()
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
		public org.lightcouch.ViewResult<K, V, T>.Rows getRows() {
			return rows;
		}

		/**
		 * @param obj
		 * @return
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			return rows.equals(obj);
		}

		/**
		 * @return
		 * @see org.lightcouch.ViewResult.Rows#getId()
		 */
		public String getId() {
			return rows.getId();
		}

		/**
		 * @return
		 * @see org.lightcouch.ViewResult.Rows#getKey()
		 */
		public K getKey() {
			return rows.getKey();
		}

		/**
		 * @return
		 * @see org.lightcouch.ViewResult.Rows#getValue()
		 */
		public V getValue() {
			return rows.getValue();
		}

		/**
		 * @return
		 * @see org.lightcouch.ViewResult.Rows#getDoc()
		 */
		public T getDoc() {
			return rows.getDoc();
		}

		/**
		 * @return
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return rows.hashCode();
		}

		/**
		 * @param id
		 * @see org.lightcouch.ViewResult.Rows#setId(java.lang.String)
		 */
		public void setId(String id) {
			rows.setId(id);
		}

		/**
		 * @param key
		 * @see org.lightcouch.ViewResult.Rows#setKey(java.lang.Object)
		 */
		public void setKey(K key) {
			rows.setKey(key);
		}

		/**
		 * @param value
		 * @see org.lightcouch.ViewResult.Rows#setValue(java.lang.Object)
		 */
		public void setValue(V value) {
			rows.setValue(value);
		}

		/**
		 * @param doc
		 * @see org.lightcouch.ViewResult.Rows#setDoc(java.lang.Object)
		 */
		public void setDoc(T doc) {
			rows.setDoc(doc);
		}

		/**
		 * @return
		 * @see org.lightcouch.ViewResult.Rows#toString()
		 */
		public String toString() {
			return rows.toString();
		}
		
		
		
	}
	
}
