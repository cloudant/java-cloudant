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

package com.cloudant.client.api.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Encapsulates a design document.
 *
 * @author Ahmed Yehia
 * @since 0.0.2
 */
public class DesignDocument extends com.cloudant.client.org.lightcouch.Document {

    private String language;
    private Map<String, MapReduce> views;
    @SerializedName("validate_doc_update")
    private String validateDocUpdate;
    private Map<String, String> filters;
    private Map<String, String> shows;
    private Map<String, String> lists;
    private Map<String, String> updates;
    private JsonArray rewrites;
    private JsonObject fulltext;
    private JsonObject indexes;

    public String getLanguage() {
        return language;
    }

    public Map<String, MapReduce> getViews() {
        return views;
    }

    public String getValidateDocUpdate() {
        return validateDocUpdate;
    }

    public JsonArray getRewrites() {
        return rewrites;
    }

    public JsonObject getFulltext() {
        return fulltext;
    }

    public JsonObject getIndexes() {
        return indexes;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    public Map<String, String> getShows() {
        return shows;
    }

    public Map<String, String> getLists() {
        return lists;
    }

    public Map<String, String> getUpdates() {
        return updates;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setViews(Map<String, MapReduce> views) {
        this.views = views;
    }

    public void setValidateDocUpdate(String validateDocUpdate) {
        this.validateDocUpdate = validateDocUpdate;
    }

    public void setRewrites(JsonArray rewrites) {
        this.rewrites = rewrites;
    }

    public void setFulltext(JsonObject fulltext) {
        this.fulltext = fulltext;
    }

    public void setIndexes(JsonObject indexes) {
        this.indexes = indexes;
    }

    public void setFilters(Map<String, String> filters) {
        this.filters = filters;
    }

    public void setShows(Map<String, String> shows) {
        this.shows = shows;
    }

    public void setLists(Map<String, String> lists) {
        this.lists = lists;
    }

    public void setUpdates(Map<String, String> updates) {
        this.updates = updates;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + (views != null ? views.hashCode() : 0);
        result = 31 * result + (validateDocUpdate != null ? validateDocUpdate.hashCode() : 0);
        result = 31 * result + (filters != null ? filters.hashCode() : 0);
        result = 31 * result + (shows != null ? shows.hashCode() : 0);
        result = 31 * result + (lists != null ? lists.hashCode() : 0);
        result = 31 * result + (updates != null ? updates.hashCode() : 0);
        result = 31 * result + (rewrites != null ? rewrites.hashCode() : 0);
        result = 31 * result + (fulltext != null ? fulltext.hashCode() : 0);
        result = 31 * result + (indexes != null ? indexes.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        DesignDocument that = (DesignDocument) o;

        if (language != null ? !language.equals(that.language) : that.language != null) {
            return false;
        }
        if (views != null ? !views.equals(that.views) : that.views != null) {
            return false;
        }
        if (validateDocUpdate != null ? !validateDocUpdate.equals(that.validateDocUpdate) : that
                .validateDocUpdate != null) {
            return false;
        }
        if (filters != null ? !filters.equals(that.filters) : that.filters != null) {
            return false;
        }
        if (shows != null ? !shows.equals(that.shows) : that.shows != null) {
            return false;
        }
        if (lists != null ? !lists.equals(that.lists) : that.lists != null) {
            return false;
        }
        if (updates != null ? !updates.equals(that.updates) : that.updates != null) {
            return false;
        }
        if (rewrites != null ? !rewrites.equals(that.rewrites) : that.rewrites != null) {
            return false;
        }
        if (fulltext != null ? !fulltext.equals(that.fulltext) : that.fulltext != null) {
            return false;
        }
        return !(indexes != null ? !indexes.equals(that.indexes) : that.indexes != null);

    }

    /**
     * Encapsulates a Map-Reduce function in a view.
     *
     * @author Ahmed Yehia
     */
    public static class MapReduce {
        private String map;
        private String reduce;
        private String dbcopy;

        public String getMap() {
            return map;
        }

        public String getReduce() {
            return reduce;
        }

        public void setMap(String map) {
            this.map = map;
        }

        public void setReduce(String reduce) {
            this.reduce = reduce;
        }

        /**
         * Set the name of a database to copy the reduced view results into.
         * <P>
         * For more information, including an explanation of the potential performance impact of
         * this option see the <a target="_blank"
         * href="https://docs.cloudant.com/creating_views.html#dbcopy">
         * Cloudant dbcopy documentation
         * </a>.
         * </P>
         *
         * @param databaseName of the database to store reduced view results in
         */
        public void setDbCopy(String databaseName) {
            this.dbcopy = databaseName;
        }

        /**
         * @return the database name where reduced view results will be stored, or null if unset
         */
        public String getDbCopy() {
            return dbcopy;
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
            MapReduce other = (MapReduce) obj;
            if (map == null) {
                if (other.map != null) {
                    return false;
                }
            } else if (!map.equals(other.map)) {
                return false;
            }
            if (reduce == null) {
                if (other.reduce != null) {
                    return false;
                }
            } else if (!reduce.equals(other.reduce)) {
                return false;
            }
            if (dbcopy == null) {
                if (other.dbcopy != null) {
                    return false;
                }
            } else if (!dbcopy.equals(other.dbcopy)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((map == null) ? 0 : map.hashCode());
            result = prime * result + ((reduce == null) ? 0 : reduce.hashCode());
            result = prime * result + ((dbcopy == null) ? 0 : dbcopy.hashCode());
            return result;
        }
    } // /class MapReduce
}
