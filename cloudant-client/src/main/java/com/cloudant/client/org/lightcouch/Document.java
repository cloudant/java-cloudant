/*
 * Copyright (C) 2011 lightcouch.org
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

package com.cloudant.client.org.lightcouch;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Convenient base class for CouchDB documents, defines the basic
 * <code>id</code>, <code>revision</code> properties, and attachments.
 *
 * @author Ahmed Yehia
 * @since 0.0.2
 */
public class Document {

    @SerializedName("_id")
    private String id;
    @SerializedName("_rev")
    private String revision;
    @SerializedName("_attachments")
    private Map<String, Attachment> attachments;

    public String getId() {
        return id;
    }

    public String getRevision() {
        return revision;
    }

    public Map<String, Attachment> getAttachments() {
        return attachments;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public void setAttachments(Map<String, Attachment> attachments) {
        this.attachments = attachments;
    }

    /**
     * Adds an in-line document attachment.
     *
     * @param name       The attachment file name
     * @param attachment
     */
    public void addAttachment(String name, Attachment attachment) {
        if (attachments == null) {
            attachments = new HashMap<String, Attachment>();
        }
        attachments.put(name, attachment);
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
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Document other = (Document) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }
}
