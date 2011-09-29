package org.lightcouch;

import com.google.gson.annotations.SerializedName;

/**
 * Holds information about a CouchDB database instance.
 * @author Ahmed Yehia
 */
public class CouchDbInfo {
	
	@SerializedName("db_name")
	private String dbName;
	@SerializedName("doc_count")
	private long docCount;
	@SerializedName("doc_del_count")
	private String docDelCount;
	@SerializedName("update_seq")
	private long updateSeq;
	@SerializedName("purge_seq")
	private long purgeSeq;
	@SerializedName("compact_running")
	private boolean compactRunning;
	@SerializedName("disk_size")
	private long diskSize;
	@SerializedName("instance_start_time")
	private long instanceStartTime;
	@SerializedName("disk_format_version")
	private int diskFormatVersion;

	public String getDbName() {
		return dbName;
	}

	public long getDocCount() {
		return docCount;
	}

	public String getDocDelCount() {
		return docDelCount;
	}

	public long getUpdateSeq() {
		return updateSeq;
	}

	public long getPurgeSeq() {
		return purgeSeq;
	}

	public boolean isCompactRunning() {
		return compactRunning;
	}

	public long getDiskSize() {
		return diskSize;
	}

	public long getInstanceStartTime() {
		return instanceStartTime;
	}

	public int getDiskFormatVersion() {
		return diskFormatVersion;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public void setDocCount(long docCount) {
		this.docCount = docCount;
	}

	public void setDocDelCount(String docDelCount) {
		this.docDelCount = docDelCount;
	}

	public void setUpdateSeq(long updateSeq) {
		this.updateSeq = updateSeq;
	}

	public void setPurgeSeq(long purgeSeq) {
		this.purgeSeq = purgeSeq;
	}

	public void setCompactRunning(boolean compactRunning) {
		this.compactRunning = compactRunning;
	}

	public void setDiskSize(long diskSize) {
		this.diskSize = diskSize;
	}

	public void setInstanceStartTime(long instanceStartTime) {
		this.instanceStartTime = instanceStartTime;
	}

	public void setDiskFormatVersion(int diskFormatVersion) {
		this.diskFormatVersion = diskFormatVersion;
	}
}
