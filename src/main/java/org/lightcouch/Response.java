package org.lightcouch;

/**
 * Represents CouchDB response as a result of a save, update or delete requests.
 * @author Ahmed Yehia
 *
 */
public class Response {
	private String id;
	private String rev;

	public String getId() {
		return id;
	}

	public String getRev() {
		return rev;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setRev(String rev) {
		this.rev = rev;
	}

	@Override
	public String toString() {
		return "Response [id=" + id + ", rev=" + rev + "]";
	}
}
