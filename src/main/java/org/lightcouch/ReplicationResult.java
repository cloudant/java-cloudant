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

import java.util.List;

import com.google.gson.annotations.SerializedName;

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

	public List<ReplicationHistory> getHistories() {
		return histories;
	}

	public void setOk(boolean ok) {
		this.ok = ok;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public void setSourceLastSeq(String sourceLastSeq) {
		this.sourceLastSeq = sourceLastSeq;
	}

	public void setHistories(List<ReplicationHistory> histories) {
		this.histories = histories;
	}

	/**
	 * Respresents a replication session history.
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
		private long startLastSeq;
		@SerializedName("end_last_seq")
		private long endLastSeq;
		@SerializedName("recorded_seq")
		private long recordedSeq;
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

		public long getStartLastSeq() {
			return startLastSeq;
		}

		public long getEndLastSeq() {
			return endLastSeq;
		}

		public long getRecordedSeq() {
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

		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;
		}

		public void setStartTime(String startTime) {
			this.startTime = startTime;
		}

		public void setEndTime(String endTime) {
			this.endTime = endTime;
		}

		public void setStartLastSeq(long startLastSeq) {
			this.startLastSeq = startLastSeq;
		}

		public void setEndLastSeq(long endLastSeq) {
			this.endLastSeq = endLastSeq;
		}

		public void setRecordedSeq(long recordedSeq) {
			this.recordedSeq = recordedSeq;
		}

		public void setMissingChecked(long missingChecked) {
			this.missingChecked = missingChecked;
		}

		public void setMissingFound(long missingFound) {
			this.missingFound = missingFound;
		}

		public void setDocsRead(long docsRead) {
			this.docsRead = docsRead;
		}

		public void setDocsWritten(long docsWritten) {
			this.docsWritten = docsWritten;
		}

		public void setDocWriteFailures(long docWriteFailures) {
			this.docWriteFailures = docWriteFailures;
		}

	}
}
