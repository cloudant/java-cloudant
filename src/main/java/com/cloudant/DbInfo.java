package com.cloudant;

import org.lightcouch.CouchDbInfo;
/**
 * Holds information about a CouchDB database instance.
 * @author Ganesh K Choudhary
 */
public class DbInfo {
	private CouchDbInfo couchDbInfo ;
	
	public DbInfo(){
		this.couchDbInfo = new CouchDbInfo();
	}
	
	DbInfo(CouchDbInfo couchDbInfo){
		this.couchDbInfo = couchDbInfo ;
	}

	public String getDbName() {
		return couchDbInfo.getDbName();
	}

	public long getDocCount() {
		return couchDbInfo.getDocCount();
	}

	public String getDocDelCount() {
		return couchDbInfo.getDocDelCount();
	}

	public String getUpdateSeq() {
		return couchDbInfo.getUpdateSeq();
	}

	public long getPurgeSeq() {
		return couchDbInfo.getPurgeSeq();
	}

	public boolean isCompactRunning() {
		return couchDbInfo.isCompactRunning();
	}

	public long getDiskSize() {
		return couchDbInfo.getDiskSize();
	}

	public long getInstanceStartTime() {
		return couchDbInfo.getInstanceStartTime();
	}

	public int getDiskFormatVersion() {
		return couchDbInfo.getDiskFormatVersion();
	}

	public String toString() {
		return couchDbInfo.toString();
	}
	
	
}
