package org.lightcouch;

import com.google.gson.annotations.SerializedName;

/**
 * Convenient base class for CouchDB documents, defines the basic id and revision properties.
 * @author Ahmed Yehia
 *
 */
public class Document {
	
	@SerializedName("_id")
	private String id;
	
	@SerializedName("_rev")
	private String revision;

	public String getId() {
		return id;
	}

	public String getRevision() {
		return revision;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Document other = (Document) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
