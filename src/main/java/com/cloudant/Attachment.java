package com.cloudant;

public class Attachment {
	private org.lightcouch.Attachment attachement ;
	
	public Attachment(){
		this.attachement = new org.lightcouch.Attachment();
	}
	
	public Attachment(String data, String contentType) {
		this.attachement = new org.lightcouch.Attachment(data,contentType);
	}
	
	Attachment(org.lightcouch.Attachment attachement){
		this.attachement = attachement ;
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return attachement.equals(obj);
	}

	/**
	 * @return
	 * @see org.lightcouch.Attachment#getData()
	 */
	public String getData() {
		return attachement.getData();
	}

	/**
	 * @return
	 * @see org.lightcouch.Attachment#getContentType()
	 */
	public String getContentType() {
		return attachement.getContentType();
	}

	/**
	 * @return
	 * @see org.lightcouch.Attachment#getRevpos()
	 */
	public int getRevpos() {
		return attachement.getRevpos();
	}

	/**
	 * @return
	 * @see org.lightcouch.Attachment#getDigest()
	 */
	public String getDigest() {
		return attachement.getDigest();
	}

	/**
	 * @return
	 * @see org.lightcouch.Attachment#getLength()
	 */
	public long getLength() {
		return attachement.getLength();
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return attachement.hashCode();
	}

	/**
	 * @return
	 * @see org.lightcouch.Attachment#isStub()
	 */
	public boolean isStub() {
		return attachement.isStub();
	}

	/**
	 * @param contentType
	 * @see org.lightcouch.Attachment#setContentType(java.lang.String)
	 */
	public void setContentType(String contentType) {
		attachement.setContentType(contentType);
	}

	/**
	 * @param data
	 * @see org.lightcouch.Attachment#setData(java.lang.String)
	 */
	public void setData(String data) {
		attachement.setData(data);
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return attachement.toString();
	}
	
	
}
