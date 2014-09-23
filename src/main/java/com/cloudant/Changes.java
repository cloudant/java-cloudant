package com.cloudant;

import org.lightcouch.ChangesResult;
import org.lightcouch.CouchDatabaseBase;
import org.lightcouch.ChangesResult.Row;
import org.lightcouch.internal.URIBuilder;

public class Changes {
	private org.lightcouch.Changes changes ;
	
	Changes(org.lightcouch.Changes changes) {
		this.changes = changes ;
	}
	
	/**
	 * @return
	 * @see org.lightcouch.Changes#continuousChanges()
	 */
	public Changes continuousChanges() {
		changes = changes.continuousChanges();
		return this ;
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return changes.equals(obj);
	}

	/**
	 * @return
	 * @see org.lightcouch.Changes#hasNext()
	 */
	public boolean hasNext() {
		return changes.hasNext();
	}

	/**
	 * @return
	 * @see org.lightcouch.Changes#next()
	 */
	public Row next() {
		return changes.next();
	}

	/**
	 * 
	 * @see org.lightcouch.Changes#stop()
	 */
	public void stop() {
		changes.stop();
	}

	/**
	 * @return
	 * @see org.lightcouch.Changes#getChanges()
	 */
	public ChangesResult getChanges() {
		return changes.getChanges();
	}

	/**
	 * @param since
	 * @return
	 * @see org.lightcouch.Changes#since(java.lang.String)
	 */
	public Changes since(String since) {
		changes = changes.since(since);
		return this ;
	}

	/**
	 * @param limit
	 * @return
	 * @see org.lightcouch.Changes#limit(int)
	 */
	public Changes limit(int limit) {
		changes =  changes.limit(limit);
		return this ;
	}

	/**
	 * @param filter
	 * @return
	 * @see org.lightcouch.Changes#filter(java.lang.String)
	 */
	public Changes filter(String filter) {
		changes = changes.filter(filter);
		return this ;
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return changes.hashCode();
	}

	/**
	 * @param heartBeat
	 * @return
	 * @see org.lightcouch.Changes#heartBeat(long)
	 */
	public Changes heartBeat(long heartBeat) {
		changes = changes.heartBeat(heartBeat);
		return this ;
	}

	/**
	 * @param timeout
	 * @return
	 * @see org.lightcouch.Changes#timeout(long)
	 */
	public Changes timeout(long timeout) {
		changes = changes.timeout(timeout);
		return this ;
	}

	/**
	 * @param includeDocs
	 * @return
	 * @see org.lightcouch.Changes#includeDocs(boolean)
	 */
	public Changes includeDocs(boolean includeDocs) {
		changes = changes.includeDocs(includeDocs);
		return this ;
	}

	/**
	 * @param style
	 * @return
	 * @see org.lightcouch.Changes#style(java.lang.String)
	 */
	public Changes style(String style) {
		changes = changes.style(style);
		return this ;
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return changes.toString();
	}
	
	
}
