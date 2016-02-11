/*
 * Copyright (c) 2015 IBM Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.cloudant.client.api.model;


/**
 * Encapsulates an in-line document attachment.
 *
 * @see com.cloudant.client.api.model.Document#addAttachment(String, Attachment)
 */
public class Attachment {
    private com.cloudant.client.org.lightcouch.Attachment attachement;

    public Attachment() {
        this.attachement = new com.cloudant.client.org.lightcouch.Attachment();
    }

    /**
     * @param data        The base64 encoded data of the attachment.
     * @param contentType The Content-Type of the attachment.
     */
    public Attachment(String data, String contentType) {
        this.attachement = new com.cloudant.client.org.lightcouch.Attachment(data, contentType);
    }

    Attachment(com.cloudant.client.org.lightcouch.Attachment attachement) {
        this.attachement = attachement;
    }

    /**
     * @return The base64 encoded data of the attachment.
     */
    public String getData() {
        return attachement.getData();
    }

    public String getContentType() {
        return attachement.getContentType();
    }

    public int getRevpos() {
        return attachement.getRevpos();
    }

    public String getDigest() {
        return attachement.getDigest();
    }

    public long getLength() {
        return attachement.getLength();
    }

    public boolean isStub() {
        return attachement.isStub();
    }

    /**
     * @param contentType the media type of the attachment
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
