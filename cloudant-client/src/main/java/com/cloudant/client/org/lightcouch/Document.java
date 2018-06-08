/*
 * Copyright © 2015, 2018 IBM Corp. All rights reserved.
 * Copyright (C) 2011 lightcouch.org
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
 * Base class for serialisation and deserialisation of Cloudant Documents.
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
    @SerializedName("_deleted")
    private boolean deleted;

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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Document document = (Document) o;

        if (deleted != document.deleted) {
            return false;
        }
        if (id != null ? !id.equals(document.id) : document.id != null) {
            return false;
        }
        if (revision != null ? !revision.equals(document.revision) : document.revision != null) {
            return false;
        }
        return attachments != null ? attachments.equals(document.attachments) : document
                .attachments == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (revision != null ? revision.hashCode() : 0);
        result = 31 * result + (attachments != null ? attachments.hashCode() : 0);
        result = 31 * result + (deleted ? 1 : 0);
        return result;
    }
}
