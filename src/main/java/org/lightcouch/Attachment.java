/*
 * Copyright (C) 2011 lightcouch.org
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

import com.google.gson.annotations.SerializedName;

/**
 * Represents an in-line document attachment. 
 * @see Document#addAttachment(String, Attachment)
 * @since 0.0.4
 * @author Ahmed Yehia
 */
public class Attachment {
	
	private String data;
	@SerializedName("content_type")
	private String contentType;
	private int revpos;
	private String digest;
	private long length;
	private boolean stub;
	
	// Constructor
	
	public Attachment() {

	}
	
	/**
	 * @param data The base64 encoded data of the attachment.
	 * @param contentType The Content-Type of the attachment.
	 */
	public Attachment(String data, String contentType) {
		this.data = data;
		this.contentType = contentType;
	}
	
	// Getter

	/**
	 * @return The base64 encoded data of the attachment.
	 */
	public String getData() {
		return data;
	}

	public String getContentType() {
		return contentType;
	}

	public int getRevpos() {
		return revpos;
	}

	public String getDigest() {
		return digest;
	}

	public long getLength() {
		return length;
	}

	public boolean isStub() {
		return stub;
	}
	
	// Setter
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * @param data The base64 encoded data of the attachment.
	 */
	public void setData(String data) {
		this.data = data;
	}
}
