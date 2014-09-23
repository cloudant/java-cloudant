package com.cloudant;

import java.io.InputStream;
import java.util.List;

import org.lightcouch.DesignDocument.MapReduce;
import org.lightcouch.Page;
import org.lightcouch.ViewResult;

public class View {
	private org.lightcouch.View view ;
	
	/**
	 * @return the view
	 */
	org.lightcouch.View getView() {
		return view;
	}

	/**
	 * @param view the view to set
	 */
	void setView(org.lightcouch.View view) {
		this.view = view;
	}

	/**
	 * @return
	 * @see org.lightcouch.View#queryForStream()
	 */
	public InputStream queryForStream() {
		return view.queryForStream();
	}

	/**
	 * @param classOfT
	 * @return
	 * @see org.lightcouch.View#query(java.lang.Class)
	 */
	public <T> List<T> query(Class<T> classOfT) {
		return view.query(classOfT);
	}

	/**
	 * @param classOfK
	 * @param classOfV
	 * @param classOfT
	 * @return
	 * @see org.lightcouch.View#queryView(java.lang.Class, java.lang.Class, java.lang.Class)
	 */
	public <K, V, T> com.cloudant.ViewResult<K, V, T> queryView(Class<K> classOfK,
			Class<V> classOfV, Class<T> classOfT) {
		 ViewResult<K,V,T> lightCouchQueryView = view.queryView(classOfK, classOfV, classOfT);
		 com.cloudant.ViewResult<K, V, T> queryView = new com.cloudant.ViewResult<>(lightCouchQueryView);
		 return queryView ;
	}

	/**
	 * @return
	 * @see org.lightcouch.View#queryForInt()
	 */
	public int queryForInt() {
		return view.queryForInt();
	}

	/**
	 * @return
	 * @see org.lightcouch.View#queryForBoolean()
	 */
	public boolean queryForBoolean() {
		return view.queryForBoolean();
	}

	/**
	 * @param key
	 * @return
	 * @see org.lightcouch.View#key(java.lang.Object[])
	 */
	public View key(Object... key) {
		this.view = view.key(key);
		return this ;
	}

	/**
	 * @param startKey
	 * @return
	 * @see org.lightcouch.View#startKey(java.lang.Object[])
	 */
	public View startKey(Object... startKey) {
		this.view =  view.startKey(startKey);
		return this ;
	}

	/**
	 * @param startKeyDocId
	 * @return
	 * @see org.lightcouch.View#startKeyDocId(java.lang.String)
	 */
	public View startKeyDocId(String startKeyDocId) {
		this.view = view.startKeyDocId(startKeyDocId);
		return this ;
	}

	/**
	 * @param endKey
	 * @return
	 * @see org.lightcouch.View#endKey(java.lang.Object[])
	 */
	public View endKey(Object... endKey) {
		this.view = view.endKey(endKey);
		return this ;
	}

	/**
	 * @param endKeyDocId
	 * @return
	 * @see org.lightcouch.View#endKeyDocId(java.lang.String)
	 */
	public View endKeyDocId(String endKeyDocId) {
		this.view = view.endKeyDocId(endKeyDocId);
		return this ;
	}

	/**
	 * @param descending
	 * @return
	 * @see org.lightcouch.View#descending(java.lang.Boolean)
	 */
	public View descending(Boolean descending) {
		this.view =  view.descending(descending);
		return this ;
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return view.equals(obj);
	}

	/**
	 * @return
	 * @see org.lightcouch.View#queryForString()
	 */
	public String queryForString() {
		return view.queryForString();
	}

	/**
	 * @param limit
	 * @return
	 * @see org.lightcouch.View#limit(java.lang.Integer)
	 */
	public View limit(Integer limit) {
		this.view =  view.limit(limit);
		return this ;
	}

	/**
	 * @param group
	 * @return
	 * @see org.lightcouch.View#group(java.lang.Boolean)
	 */
	public View group(Boolean group) {
		this.view =  view.group(group);
		return this ;
	}

	/**
	 * @param groupLevel
	 * @return
	 * @see org.lightcouch.View#groupLevel(java.lang.Integer)
	 */
	public View groupLevel(Integer groupLevel) {
		this.view =  view.groupLevel(groupLevel);
		return this ;
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return view.hashCode();
	}

	/**
	 * @return
	 * @see org.lightcouch.View#queryForLong()
	 */
	public long queryForLong() {
		return view.queryForLong();
	}

	/**
	 * @param rowsPerPage
	 * @param param
	 * @param classOfT
	 * @return
	 * @see org.lightcouch.View#queryPage(int, java.lang.String, java.lang.Class)
	 */
	public <T> Page<T> queryPage(int rowsPerPage, String param,
			Class<T> classOfT) {
		return view.queryPage(rowsPerPage, param, classOfT);
	}

	/**
	 * @param stale
	 * @return
	 * @see org.lightcouch.View#stale(java.lang.String)
	 */
	public View stale(String stale) {
		this.view = view.stale(stale);
		return this ;
	}

	/**
	 * @param skip
	 * @return
	 * @see org.lightcouch.View#skip(java.lang.Integer)
	 */
	public View skip(Integer skip) {
		this.view =  view.skip(skip);
		return this ;
	}

	/**
	 * @param reduce
	 * @return
	 * @see org.lightcouch.View#reduce(java.lang.Boolean)
	 */
	public View reduce(Boolean reduce) {
		this.view =  view.reduce(reduce);
		return this ;
	}

	/**
	 * @param includeDocs
	 * @return
	 * @see org.lightcouch.View#includeDocs(java.lang.Boolean)
	 */
	public View includeDocs(Boolean includeDocs) {
		this.view =  view.includeDocs(includeDocs);
		return this ;
	}

	/**
	 * @param inclusiveEnd
	 * @return
	 * @see org.lightcouch.View#inclusiveEnd(java.lang.Boolean)
	 */
	public View inclusiveEnd(Boolean inclusiveEnd) {
		this.view =  view.inclusiveEnd(inclusiveEnd);
		return this ;
	}

	/**
	 * @param updateSeq
	 * @return
	 * @see org.lightcouch.View#updateSeq(java.lang.Boolean)
	 */
	public View updateSeq(Boolean updateSeq) {
		this.view =  view.updateSeq(updateSeq);
		return this ;
	}

	/**
	 * @param keys
	 * @return
	 * @see org.lightcouch.View#keys(java.util.List)
	 */
	public View keys(List<String> keys) {
		this.view =  view.keys(keys);
		return this ;
	}

	/**
	 * @param id
	 * @return
	 * @see org.lightcouch.View#tempView(java.lang.String)
	 */
	public View tempView(String id) {
		this.view =  view.tempView(id);
		return this ;
	}

	/**
	 * @param mapReduce
	 * @return
	 * @see org.lightcouch.View#tempView(org.lightcouch.DesignDocument.MapReduce)
	 */
	public View tempView(MapReduce mapReduce) {
		this.view =  view.tempView(mapReduce);
		return this ;
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return view.toString();
	}
	
	
}
