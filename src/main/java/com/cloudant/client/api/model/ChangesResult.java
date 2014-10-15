package com.cloudant.client.api.model;

import java.util.ArrayList;
import java.util.List;

import org.lightcouch.Changes;

import com.google.gson.JsonObject;
/**
 * Represents Changes feed result of type <i>normal</i>.
 * @since 0.0.1
 * @see Changes
 * @author Ganesh K Choudhary
 *
 */
public class ChangesResult {
	
	private org.lightcouch.ChangesResult changesResult ;
	
	public ChangesResult(){
		this.changesResult = new org.lightcouch.ChangesResult();
	}
	
	public ChangesResult(org.lightcouch.ChangesResult changesResult){
		this.changesResult = changesResult ;
	}

	public List<Row> getResults() {
		List<org.lightcouch.ChangesResult.Row> lightCouchResults = changesResult.getResults();
		List<Row> rows = new ArrayList<Row>();
		for(org.lightcouch.ChangesResult.Row couchRow : lightCouchResults ){
			Row row = new Row(couchRow);
			rows.add(row);
		}
		return rows ;
	}

	public String getLastSeq() {
		return changesResult.getLastSeq();
	}

		
	/**
	 * Represent a row in Changes result. 
	 */
	public static class Row {
		private org.lightcouch.ChangesResult.Row row ;
		
		public Row(org.lightcouch.ChangesResult.Row row){
			this.row = row ;
		}
		
		
		public String getSeq() {
			return row.getSeq();
		}



		public String getId() {
			return row.getId();
		}



		public List<Rev> getChanges() {
			List<org.lightcouch.ChangesResult.Row.Rev> lightCouchChanges = row.getChanges();
			List<Rev> changes = new ArrayList<Rev>();
			for(org.lightcouch.ChangesResult.Row.Rev rev : lightCouchChanges){
				changes.add(new Rev(rev));
			}
			return changes ;
		}

		public JsonObject getDoc() {
			return row.getDoc();
		}


		public boolean isDeleted() {
			return row.isDeleted();
		}

		/**
		 * Represent a Change rev. 
		 */
		public static class Rev {
			private org.lightcouch.ChangesResult.Row.Rev rev ;
			
			public Rev(org.lightcouch.ChangesResult.Row.Rev rev){
				this.rev = rev ;
			}

			public String getRev() {
				return rev.getRev();
			}
		}
	}
	
}
