package com.cloudant;

import java.util.ArrayList;
import java.util.List;

public class ReplicationResult {
	private org.lightcouch.ReplicationResult replicationResult ;
	
	public ReplicationResult(){
		replicationResult = new org.lightcouch.ReplicationResult();
	}
	
	ReplicationResult(org.lightcouch.ReplicationResult replicationResult){
		this.replicationResult = replicationResult ;
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return replicationResult.equals(obj);
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicationResult#isOk()
	 */
	public boolean isOk() {
		return replicationResult.isOk();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicationResult#getSessionId()
	 */
	public String getSessionId() {
		return replicationResult.getSessionId();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicationResult#getSourceLastSeq()
	 */
	public String getSourceLastSeq() {
		return replicationResult.getSourceLastSeq();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicationResult#getLocalId()
	 */
	public String getLocalId() {
		return replicationResult.getLocalId();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicationResult#getHistories()
	 */
	public List<ReplicationHistory> getHistories() {
		List<org.lightcouch.ReplicationResult.ReplicationHistory> couchDbreplicationHistories = replicationResult.getHistories();
		List<ReplicationHistory> histories = new ArrayList<>();
		for(org.lightcouch.ReplicationResult.ReplicationHistory couchDbReplicationHistory : couchDbreplicationHistories){
		ReplicationHistory replicationHistory = new ReplicationHistory(couchDbReplicationHistory);
			histories.add(replicationHistory);
		}
		return histories ;
		
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return replicationResult.hashCode();
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return replicationResult.toString();
	}
	
	/**
	 * Represents a replication session history.
	 * @author Ahmed Yehia
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
		 * @param obj
		 * @return
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			return replicationHistory.equals(obj);
		}

		/**
		 * @return
		 * @see org.lightcouch.ReplicationResult.ReplicationHistory#getSessionId()
		 */
		public String getSessionId() {
			return replicationHistory.getSessionId();
		}

		/**
		 * @return
		 * @see org.lightcouch.ReplicationResult.ReplicationHistory#getStartTime()
		 */
		public String getStartTime() {
			return replicationHistory.getStartTime();
		}

		/**
		 * @return
		 * @see org.lightcouch.ReplicationResult.ReplicationHistory#getEndTime()
		 */
		public String getEndTime() {
			return replicationHistory.getEndTime();
		}

		/**
		 * @return
		 * @see org.lightcouch.ReplicationResult.ReplicationHistory#getStartLastSeq()
		 */
		public String getStartLastSeq() {
			return replicationHistory.getStartLastSeq();
		}

		/**
		 * @return
		 * @see org.lightcouch.ReplicationResult.ReplicationHistory#getEndLastSeq()
		 */
		public String getEndLastSeq() {
			return replicationHistory.getEndLastSeq();
		}

		/**
		 * @return
		 * @see org.lightcouch.ReplicationResult.ReplicationHistory#getRecordedSeq()
		 */
		public String getRecordedSeq() {
			return replicationHistory.getRecordedSeq();
		}

		/**
		 * @return
		 * @see org.lightcouch.ReplicationResult.ReplicationHistory#getMissingChecked()
		 */
		public long getMissingChecked() {
			return replicationHistory.getMissingChecked();
		}

		/**
		 * @return
		 * @see org.lightcouch.ReplicationResult.ReplicationHistory#getMissingFound()
		 */
		public long getMissingFound() {
			return replicationHistory.getMissingFound();
		}

		/**
		 * @return
		 * @see org.lightcouch.ReplicationResult.ReplicationHistory#getDocsRead()
		 */
		public long getDocsRead() {
			return replicationHistory.getDocsRead();
		}

		/**
		 * @return
		 * @see org.lightcouch.ReplicationResult.ReplicationHistory#getDocsWritten()
		 */
		public long getDocsWritten() {
			return replicationHistory.getDocsWritten();
		}

		/**
		 * @return
		 * @see org.lightcouch.ReplicationResult.ReplicationHistory#getDocWriteFailures()
		 */
		public long getDocWriteFailures() {
			return replicationHistory.getDocWriteFailures();
		}

		/**
		 * @return
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return replicationHistory.hashCode();
		}

		/**
		 * @return
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return replicationHistory.toString();
		}
		
		
	}
	
}
