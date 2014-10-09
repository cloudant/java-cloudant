package com.cloudant.client.api;



import com.cloudant.client.api.model.ChangesResult;
import com.cloudant.client.api.model.ChangesResult.Row;
/**
 * <p>Contains the Change Notifications API, supports <i>normal</i> and <i>continuous</i> feed Changes. 
 * <h3>Usage Example:</h3>
 * <pre>
 * // feed type normal 
 * String since = db.info().getUpdateSeq(); // latest update seq
 * ChangesResult changeResult = db.changes()
 *	.since(since) 
 *	.limit(10)
 *	.filter("example/filter")
 *	.getChanges();
 *
 * for (ChangesResult.Row row : changeResult.getResults()) {
 *   String docId = row.getId()
 *   JsonObject doc = row.getDoc();
 * }
 *
 * // feed type continuous
 * Changes changes = db.changes()
 *	.includeDocs(true) 
 *	.heartBeat(30000)
 *	.continuousChanges(); 
 * 
 * while (changes.hasNext()) { 
 *	ChangesResult.Row feed = changes.next();
 *  String docId = feed.getId();
 *  JsonObject doc = feed.getDoc();
 *	// changes.stop(); // stop continuous feed
 * }
 * </pre>
 * @see ChangesResult
 * @since 0.0.1
 * @author Ganesh K Choudhary
 */
public class Changes {
	private org.lightcouch.Changes changes ;
	
	Changes(org.lightcouch.Changes changes) {
		this.changes = changes ;
	}
	
	/**
	 * Requests Change notifications of feed type continuous.
	 * <p>Feed notifications are accessed in an <i>iterator</i> style.
	 */
	public Changes continuousChanges() {
		changes = changes.continuousChanges();
		return this ;
	}

	/**
	 * Checks whether a feed is available in the continuous stream, blocking 
	 * until a feed is received. 
	 */
	public boolean hasNext() {
		return changes.hasNext();
	}

	/**
	 * @return The next feed in the stream.
	 */
	public Row next() {
		org.lightcouch.ChangesResult.Row next = changes.next();
		Row row = new Row(next);
		return row ;
	}

	/**
	 * Stops a running continuous feed.
	 */
	public void stop() {
		changes.stop();
	}

	/**
	 * Requests Change notifications of feed type normal.
	 */	 
	public ChangesResult getChanges() {
		org.lightcouch.ChangesResult couchDbChangesResult = changes.getChanges();
		ChangesResult changeResult = new ChangesResult(couchDbChangesResult);
		return changeResult ;
	}

	// Query Params
	
	public Changes since(String since) {
		changes = changes.since(since);
		return this ;
	}

	
	public Changes limit(int limit) {
		changes =  changes.limit(limit);
		return this ;
	}

	
	public Changes filter(String filter) {
		changes = changes.filter(filter);
		return this ;
	}

	
	
	public Changes heartBeat(long heartBeat) {
		changes = changes.heartBeat(heartBeat);
		return this ;
	}

	
	public Changes timeout(long timeout) {
		changes = changes.timeout(timeout);
		return this ;
	}

	
	public Changes includeDocs(boolean includeDocs) {
		changes = changes.includeDocs(includeDocs);
		return this ;
	}

	
	public Changes style(String style) {
		changes = changes.style(style);
		return this ;
	}

	
	
	
	
}
