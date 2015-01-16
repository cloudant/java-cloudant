package com.cloudant.client.api.model;

import java.util.List;

import com.cloudant.client.api.Database;


/**
 * Query parameters to append to find requests.
 * <p>Example: 
 * <pre>
 * Database.find(Foo.class, "doc-id", new Params().readQuorum(3));
 * </pre>
 * @see Database#find(Class, String, Params)
 * @since 0.0.1
 * @author Mario Briggs
 * 
 */
public class Params  {

	private com.cloudant.client.org.lightcouch.Params params = new com.cloudant.client.org.lightcouch.Params();
	
	public Params readQuorum(int quorum) {
		params.addParam("r", new Integer(quorum).toString());
		return this;
	}

	public Params attachments() {
		params =  params.attachments();
		return this ;
	}

	public Params revisions() {
		params = params.revisions();
		return this ;
	}

	public Params conflicts() {
		params = params.conflicts();
		return this ;
	}

	public Params addParam(String name, String value) {
		params = params.addParam(name, value);
		return this ;
	}

	public boolean equals(Object obj) {
		return params.equals(obj);
	}

	public List<String> getParams() {
		return params.getParams();
	}

	public int hashCode() {
		return params.hashCode();
	}

	public Params revsInfo() {
		params = params.revsInfo();
		return this ;
	}

	public Params rev(String rev) {
		params = params.rev(rev);
		return this ;
	}

	public Params localSeq() {
		params = params.localSeq();
		return this ;
	}

	public String toString() {
		return params.toString();
	}
	
	public com.cloudant.client.org.lightcouch.Params getInternalParams() {
		return params;
	}
	
	
	
}
