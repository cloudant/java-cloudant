/*
 * Copyright (C) 2011 Ahmed Yehia (ahmed.yehia.m@gmail.com)
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
 * <p>Represents an in-line document attachment. 
 * <p>On saving, the fields 'data' holds the base64 encoded data, 
 * and 'contentType' holds the Content-Type of the attachment.
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

	public void setData(String data) {
		this.data = data;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setRevpos(int revpos) {
		this.revpos = revpos;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public void setStub(boolean stub) {
		this.stub = stub;
	}
}
