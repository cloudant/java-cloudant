package com.cloudant.client.api.model;


import com.cloudant.client.org.lightcouch.Document;

/**
 * Represents an in-line document attachment. 
 * @see Document#addAttachment(String, Attachment)
 */
public class Attachment {
	private com.cloudant.client.org.lightcouch.Attachment attachement ;
	
	public Attachment(){
		this.attachement = new com.cloudant.client.org.lightcouch.Attachment();
	}
	
	/**
	 * @param data The base64 encoded data of the attachment.
	 * @param contentType The Content-Type of the attachment.
	 */
	public Attachment(String data, String contentType) {
		this.attachement = new com.cloudant.client.org.lightcouch.Attachment(data,contentType);
	}
	
	Attachment(com.cloudant.client.org.lightcouch.Attachment attachement){
		this.attachement = attachement ;
	}

	/**
	 * @return The base64 encoded data of the attachment.
	 */
	public String getData() {
		return attachement.getData();
	}

	/**
	 * @return
	 */
	public String getContentType() {
		return attachement.getContentType();
	}

	/**
	 * @return
	 */
	public int getRevpos() {
		return attachement.getRevpos();
	}

	/**
	 * @return
	 */
	public String getDigest() {
		return attachement.getDigest();
	}

	/**
	 * @return
	 */
	public long getLength() {
		return attachement.getLength();
	}

	/**
	 * @return
	 */
	public boolean isStub() {
		return attachement.isStub();
	}

	/**
	 * @param contentType
	 */
	public void setContentType(String contentType) {
		attachement.setContentType(contentType);
	}

	/**
	 * @param data The base64 encoded data of the attachment.
	 */
	public void setData(String data) {
		attachement.setData(data);
	}
	
	com.cloudant.client.org.lightcouch.Attachment getAttachement() {
		return attachement;
	}

}
