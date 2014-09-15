package com.cloudant;

import java.util.List;


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

	private org.lightcouch.Params params = new org.lightcouch.Params();
	
	public Params readQuorum(int quorum) {
		params.addParam("r", new Integer(quorum).toString());
		return this;
	}

	public org.lightcouch.Params attachments() {
		return params.attachments();
	}

	public org.lightcouch.Params revisions() {
		return params.revisions();
	}

	public org.lightcouch.Params conflicts() {
		return params.conflicts();
	}

	public org.lightcouch.Params addParam(String name, String value) {
		return params.addParam(name, value);
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

	public org.lightcouch.Params revsInfo() {
		return params.revsInfo();
	}

	public org.lightcouch.Params rev(String rev) {
		return params.rev(rev);
	}

	public org.lightcouch.Params localSeq() {
		return params.localSeq();
	}

	public String toString() {
		return params.toString();
	}
	
	org.lightcouch.Params getInternalParams() {
		return params;
	}
	
	
	
}
