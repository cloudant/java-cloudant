/*
 * Copyright © 2015, 2019 IBM Corp. All rights reserved.
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
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;

/**
 * Encapsulates information about a database instance.
 *
 * @author Ganesh K Choudhary
 */
public class DbInfo {

    /**
     * Encapsulates partitioned index properties.
     */
    public static class PartitionedIndexes {

        /**
         * Encapsulates index properties.
         */
        public static class Indexes {

            private long search;
            private long view;


            /**
             * Get a count of the partitioned search indexes.
             *
             * @return The partitioned search index count.
             */
            public long getSearch() {
                return search;
            }


            /**
             * Get a count of the partitioned view indexes.
             *
             * @return The partitioned view index count.
             */
            public long getView() {
                return view;
            }

        }

        private long count;
        private long limit;
        private Indexes indexes;

        /**
         * Get a total count of the partitioned indexes.
         *
         * @return The total partitioned index count.
         */
        public long getCount() {
            return count;
        }

        /**
         * Get the partitioned index limit.
         *
         * @return The partitioned index limit.
         */
        public long getLimit() {
            return limit;
        }

        /**
         * Get the {@link com.cloudant.client.api.model.DbInfo.PartitionedIndexes.Indexes} object
         * for this database.
         *
         * @return The {@link com.cloudant.client.api.model.DbInfo.PartitionedIndexes.Indexes}
         * object, containing the count breakdown of partitioned indexes.
         */
        public Indexes getIndexes() {
            return indexes;
        }

    }

    /**
     * Encapsulates database properties.
     */
    public static class Props {

        private boolean partitioned = false;

        /**
         * Get the database partitioned property.
         *
         * @return database partition property
         */
        public boolean getPartitioned() {
            return partitioned;
        }
    }

    @SerializedName("db_name")
    private String dbName;
    @SerializedName("doc_count")
    private long docCount;
    @SerializedName("doc_del_count")
    private long docDelCount;
    @SerializedName("update_seq")
    private JsonElement updateSeq;
    @SerializedName("purge_seq")
    private JsonElement purgeSeq;
    @SerializedName("compact_running")
    private boolean compactRunning;
    @SerializedName("disk_size")
    private long diskSize;
    @SerializedName("instance_start_time")
    private long instanceStartTime;
    @SerializedName("disk_format_version")
    private int diskFormatVersion;
    private Props props;
    @SerializedName("partitioned_indexes")
    private PartitionedIndexes partitionedIndexes;

    public String getDbName() {
        return dbName;
    }

    public long getDocCount() {
        return docCount;
    }

    /**
     *
     * @return string form of the number of deleted documents in the database
     *
     * @see DbInfo#getDocDelCountLong
     */
    @Deprecated
    public String getDocDelCount() {
        return Long.toString(docDelCount);
    }

    /**
     *
     * @return number of deleted documents in the database
     */
    public long getDocDelCountLong() {
        return docDelCount;
    }

    public String getUpdateSeq() {
        return updateSeq.toString();
    }

    /**
     * Use {@link #getStringPurgeSeq()} instead.
     *
     * The value 0 is returned if the {@code purged_seq} field cannot be cast as a primitive long.
     * In later versions of CouchDB (&gt;=2.3.x) {@code purged_seq} is an opaque string.
     *
     * @return Number of purge operations on the database.
     */
    @Deprecated
    public long getPurgeSeq() {
        try {
            JsonPrimitive purgeSeqPrim = purgeSeq.getAsJsonPrimitive();
            return purgeSeqPrim.getAsLong();
        } catch (IllegalStateException e) {
            // Suppress exception if the element is of type JsonArray but contains more than a
            // single element.
        } catch (NumberFormatException e) {
            // Suppress exception if the element is not a JsonPrimitive and is not a valid long
            // value.
        }
        // Return 0 when value cannot be cast as a primitive long value.
        return 0;
    }

    /**
     * An opaque string that describes the state of purge operations across the database.
     *
     * @return Purge sequence.
     */
    public String getStringPurgeSeq() {
        return purgeSeq.toString();
    }

    public boolean isCompactRunning() {
        return compactRunning;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public long getInstanceStartTime() {
        return instanceStartTime;
    }

    public int getDiskFormatVersion() {
        return diskFormatVersion;
    }

    /**
     * Get the database properties.
     *
     * @return database properties
     */
    public Props getProps() {
        return props;
    }

    /**
     * Get the {@link com.cloudant.client.api.model.DbInfo.PartitionedIndexes} object for
     * this database.
     *
     * @return The {@link com.cloudant.client.api.model.DbInfo.PartitionedIndexes} object,
     * containing metadata about partitioned indexes in this database.
     */
    public PartitionedIndexes getPartitionedIndexes() {
        return partitionedIndexes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb
                .append("CouchDbInfo [")
                .append("dbName=").append(dbName)
                .append(", docCount=").append(docCount)
                .append(", docDelCount=").append(docDelCount)
                .append(", updateSeq=").append(updateSeq)
                .append(", purgeSeq=").append(purgeSeq)
                .append(", compactRunning=").append(compactRunning)
                .append(", diskSize=").append(diskSize)
                .append(", instanceStartTime=").append(instanceStartTime)
                .append(", diskFormatVersion=").append(diskFormatVersion);

        if (this.getProps() != null) {
            sb.append(", props.partitioned=").append(this.getProps().getPartitioned());
        }

        if (this.getPartitionedIndexes() != null) {
            sb
                    .append(", partitionedIndexes.count=")
                    .append(this.getPartitionedIndexes().getCount())
                    .append(", partitionedIndexes.limit=")
                    .append(this.getPartitionedIndexes().getLimit())
                    .append(", partitionedIndexes.indexes.search=")
                    .append(this.getPartitionedIndexes().getIndexes().getSearch())
                    .append(", partitionedIndexes.indexes.view=")
                    .append(this.getPartitionedIndexes().getIndexes().getView());
        }

        sb.append("]");

        return sb.toString();
    }

}
