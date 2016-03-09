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

import com.google.gson.JsonElement;

import java.util.Date;

/**
 * Encapsulates info about Cloudant Server Task
 *
 * @author Mario Briggs
 * @since 0.0.1
 */
public class Task {

    private String user;
    private String type;
    private long updated_on;
    private String pid;
    private String node;
    private long started_on;

    private String target;
    private long docs_read;
    private long doc_write_failures;
    private String doc_id;
    private boolean continuous;
    private JsonElement checkpointed_source_seq;
    private long changes_pending;
    private long docs_written;
    private long missing_revisions_found;
    private String replication_id;
    private long revisions_checked;
    private String source;
    private JsonElement source_seq;


    private long changes_done;
    private String database;
    private String design_document;
    private long total_changes;
    private String index;

    private long view;
    private String phase;

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the updated_on
     */
    public Date getUpdated_on() {
        return new Date(updated_on * 1000);
    }

    /**
     * @return the pid
     */
    public String getPid() {
        return pid;
    }

    /**
     * @return the node
     */
    public String getNode() {
        return node;
    }

    /**
     * @return the started_on
     */
    public Date getStarted_on() {
        return new Date(started_on * 1000);
    }

    /**
     * @return the target
     */
    public String getTarget() {
        return target;
    }

    /**
     * @return the docs_read
     */
    public long getDocs_read() {
        return docs_read;
    }

    /**
     * @return the doc_write_failures
     */
    public long getDoc_write_failures() {
        return doc_write_failures;
    }

    /**
     * @return the doc_id
     */
    public String getDoc_id() {
        return doc_id;
    }

    /**
     * @return the continuous
     */
    public boolean isContinuous() {
        return continuous;
    }

    /**
     * @return the checkpointed_source_seq
     */
    public String getCheckpointed_source_seq() {
        return checkpointed_source_seq.toString();
    }

    /**
     * @return the changes_pending
     */
    public long getChanges_pending() {
        return changes_pending;
    }

    /**
     * @return the docs_written
     */
    public long getDocs_written() {
        return docs_written;
    }

    /**
     * @return the missing_revisions_found
     */
    public long getMissing_revisions_found() {
        return missing_revisions_found;
    }

    /**
     * @return the replication_id
     */
    public String getReplication_id() {
        return replication_id;
    }

    /**
     * @return the revisions_checked
     */
    public long getRevisions_checked() {
        return revisions_checked;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @return the source_seq
     */
    public String getSource_seq() {
        return source_seq.toString();
    }

    /**
     * @return the changes_done
     */
    public long getChanges_done() {
        return changes_done;
    }

    /**
     * @return the database
     */
    public String getDatabase() {
        return database;
    }

    /**
     * @return the design_document
     */
    public String getDesign_document() {
        return design_document;
    }

    /**
     * @return the total_changes
     */
    public long getTotal_changes() {
        return total_changes;
    }

    /**
     * @return the index
     */
    public String getIndex() {
        return index;
    }

    /**
     * @return the view
     */
    public long getView() {
        return view;
    }

    /**
     * @return the phase
     */
    public String getPhase() {
        return phase;
    }

    Task() {
        super();
    }

}
