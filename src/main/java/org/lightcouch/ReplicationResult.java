/*
 * Copyright (C) 2011 Ahmed Yehia (ahmed.yehia.m@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lightcouch;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Holds the result of a replication request, along with previous sessions history.
 * @see Replication
 * @author Ahmed Yehia
 */
public class ReplicationResult {
	@SerializedName("ok")
	private boolean ok;
	@SerializedName("session_id")
	private String sessionId;
	@SerializedName("source_last_seq")
	private String sourceLastSeq;
	@SerializedName("_local_id")
	private String localId;
	@SerializedName("history")
	private List<ReplicationHistory> histories;

	public boolean isOk() {
		return ok;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getSourceLastSeq() {
		return sourceLastSeq;
	}
	
	public String getLocalId() {
		return localId;
	}

	public List<ReplicationHistory> getHistories() {
		return histories;
	}

	/**
	 * Represents a replication session history.
	 * @author Ahmed Yehia
	 */
	public static class ReplicationHistory {
		@SerializedName("session_id")
		private String sessionId;
		@SerializedName("start_time")
		private String startTime;
		@SerializedName("end_time")
		private String endTime;
		@SerializedName("start_last_seq")
		private String startLastSeq;
		@SerializedName("end_last_seq")
		private String endLastSeq;
		@SerializedName("recorded_seq")
		private String recordedSeq;
		@SerializedName("missing_checked")
		private long missingChecked;
		@SerializedName("missing_found")
		private long missingFound;
		@SerializedName("docs_read")
		private long docsRead;
		@SerializedName("docs_written")
		private long docsWritten;
		@SerializedName("doc_write_failures")
		private long docWriteFailures;

		public String getSessionId() {
			return sessionId;
		}

		public String getStartTime() {
			return startTime;
		}

		public String getEndTime() {
			return endTime;
		}

		public String getStartLastSeq() {
			return startLastSeq;
		}

		public String getEndLastSeq() {
			return endLastSeq;
		}

		public String getRecordedSeq() {
			return recordedSeq;
		}

		public long getMissingChecked() {
			return missingChecked;
		}

		public long getMissingFound() {
			return missingFound;
		}

		public long getDocsRead() {
			return docsRead;
		}

		public long getDocsWritten() {
			return docsWritten;
		}

		public long getDocWriteFailures() {
			return docWriteFailures;
		}
	} // /class ReplicationHistory

} 
