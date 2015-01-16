package com.cloudant.client.api.model;

import java.util.ArrayList;
import java.util.List;

import org.lightcouch.Replication;
/**
 * Holds the result of a replication request, along with previous sessions history.
 * @see Replication
 * @since 0.0.1
 * @author Ganesh K Choudhary
 */
public class ReplicationResult {
	private org.lightcouch.ReplicationResult replicationResult ;
	
	public ReplicationResult(){
		replicationResult = new org.lightcouch.ReplicationResult();
	}
	
	public ReplicationResult(org.lightcouch.ReplicationResult replicationResult){
		this.replicationResult = replicationResult ;
	}

	/**
	 * @return
	 */
	public boolean isOk() {
		return replicationResult.isOk();
	}

	/**
	 * @return
	 */
	public String getSessionId() {
		return replicationResult.getSessionId();
	}

	/**
	 * @return
	 */
	public String getSourceLastSeq() {
		return replicationResult.getSourceLastSeq();
	}

	/**
	 * @return
	 */
	public String getLocalId() {
		return replicationResult.getLocalId();
	}

	/**
	 * @return
	 */
	public List<ReplicationHistory> getHistories() {
		List<org.lightcouch.ReplicationResult.ReplicationHistory> couchDbreplicationHistories = replicationResult.getHistories();
		List<ReplicationHistory> histories = new ArrayList<ReplicationHistory>();
		for(org.lightcouch.ReplicationResult.ReplicationHistory couchDbReplicationHistory : couchDbreplicationHistories){
		ReplicationHistory replicationHistory = new ReplicationHistory(couchDbReplicationHistory);
			histories.add(replicationHistory);
		}
		return histories ;
		
	}


	
	/**
	 * Represents a replication session history.
	 * @author Ganesh K Choudhary
	 */
	public static class ReplicationHistory {
		private org.lightcouch.ReplicationResult.ReplicationHistory replicationHistory ;
		
		public ReplicationHistory(){
			// default constructor
		}
		
		public ReplicationHistory(
				org.lightcouch.ReplicationResult.ReplicationHistory replicationHistory) {
			this.replicationHistory = replicationHistory;
		}

		/**
		 * @return
		 */
		public String getSessionId() {
			return replicationHistory.getSessionId();
		}

		/**
		 * @return
		 */
		public String getStartTime() {
			return replicationHistory.getStartTime();
		}

		/**
		 * @return
		 */
		public String getEndTime() {
			return replicationHistory.getEndTime();
		}

		/**
		 * @return
		 */
		public String getStartLastSeq() {
			return replicationHistory.getStartLastSeq();
		}

		/**
		 * @return
		 */
		public String getEndLastSeq() {
			return replicationHistory.getEndLastSeq();
		}

		/**
		 * @return
		 */
		public String getRecordedSeq() {
			return replicationHistory.getRecordedSeq();
		}

		/**
		 * @return
		 */
		public long getMissingChecked() {
			return replicationHistory.getMissingChecked();
		}

		/**
		 * @return
		 */
		public long getMissingFound() {
			return replicationHistory.getMissingFound();
		}

		/**
		 * @return
		 */
		public long getDocsRead() {
			return replicationHistory.getDocsRead();
		}

		/**
		 * @return
		 */
		public long getDocsWritten() {
			return replicationHistory.getDocsWritten();
		}

		/**
		 * @return
		 */
		public long getDocWriteFailures() {
			return replicationHistory.getDocWriteFailures();
		}

			
		
	}
	
}
