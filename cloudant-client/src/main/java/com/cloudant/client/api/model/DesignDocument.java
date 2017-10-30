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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Encapsulates a design document.
 * <P>
 * This is the type of design document objects used by the
 * {@link com.cloudant.client.api.DesignDocumentManager}.
 * </P>
 * <P>
 * This class can be used as a deserialization target for design documents stored in a database.
 * For example:
 * </P>
 * <pre>
 * {@code
 * DesignDocument exampleDDoc = database.find(DesignDocument.class, "_design/exampleDesignDoc");
 * }
 * </pre>
 * <P>
 * This class can also be used to create or update design documents.
 * For example:
 * </P>
 * <pre>
 * {@code
 * // Create
 * DesignDocument ddoc = new DesignDocument();
 * // Call setters on ddoc to populate document
 * ddoc.setId("_design/exampleDesignDoc");
 * // Save to create the design document
 * database.save(ddoc)
 *
 * // Update
 * DesignDocument ddoc = database.find(DesignDocument.class, "_design/exampleDesignDoc");
 * // Call setters to update values
 * Map<String, String> updates = new HashMap<String, String>();
 * updates.put("newUpdateHandler", "function (doc, req) { ... }");
 * ddoc.setUpdates(updates);
 * // Update the design document
 * database.update(ddoc);
 *
 * }
 * </pre>
 *
 * @author Ahmed Yehia
 * @see com.cloudant.client.api.DesignDocumentManager
 * @since 0.0.2
 */
public class DesignDocument extends com.cloudant.client.org.lightcouch.Document {

    private static final String LANG_QUERY = "query";
    // Default GSON instance for serializing/deserializing JsonElements of views
    private static final Gson GSON = new Gson();

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

    /**
     * Get the language used for the views defined in this design document.
     *
     * @return typically either "javascript" or "query".
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Get the views defined in this design document.
     *
     * @return a map of view name to {@link MapReduce}
     * from the view
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#views"
     * target="_blank">Views</a>
     */
    public Map<String, MapReduce> getViews() {
        return views;
    }

    /**
     * Get the string of the javascript function set for the design document's {@code
     * validate_doc_update} property.
     *
     * @return string of validate_doc_update function
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#update-validators"
     * target="_blank">Update validators</a>
     */
    public String getValidateDocUpdate() {
        return validateDocUpdate;
    }

    /**
     * Get the array of URL rewriting rules set in the design document's {@code rewrites} property.
     *
     * @return array of JSON objects each representing a rewrite rule
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#rewrite-rules"
     * target="_blank">Rewrite rules</a>
     */
    public JsonArray getRewrites() {
        return rewrites;
    }

    /**
     * @return the design document's {@code fulltext} property
     */
    public JsonObject getFulltext() {
        return fulltext;
    }

    /**
     * Get a JSON object containing all the indexes defined in the design document.
     *
     * @return the JSON object stored in the design document's {@code indexes} property
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#indexes"
     * target="_blank">Indexes</a>
     */
    public JsonObject getIndexes() {
        return indexes;
    }

    /**
     * Get the changes feed filter functions defined in this design document.
     *
     * @return map of filter name to function string
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#filter-functions"
     * target="_blank">Filter functions</a>
     */
    public Map<String, String> getFilters() {
        return filters;
    }

    /**
     * Get the show functions defined in this design document.
     *
     * @return map of show function name to function string
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#show-functions"
     * target="_blank">Show functions</a>
     */
    public Map<String, String> getShows() {
        return shows;
    }

    /**
     * Get the list functions defined in this design document.
     *
     * @return map of list function name to function string
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#list-functions"
     * target="_blank">List functions</a>
     */
    public Map<String, String> getLists() {
        return lists;
    }

    /**
     * Get the update handlers defined in this design document.
     *
     * @return map of update handler name to function string
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#update-handlers"
     * target="_blank">Update handlers</a>
     */
    public Map<String, String> getUpdates() {
        return updates;
    }

    /**
     * Set the language of the design document.
     *
     * @param language typically {@code "javascript"}
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Set the views defined in this design document's view property.
     *
     * @param views map of view name to MapReduce class
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#views"
     * target="_blank">Views</a>
     */
    public void setViews(Map<String, MapReduce> views) {
        this.views = views;
    }

    /**
     * Set the javascript function for the design document's {@code validate_doc_update} property.
     *
     * @param validateDocUpdate string defining validate_doc_update function
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#update-validators"
     * target="_blank">Update validators</a>
     */
    public void setValidateDocUpdate(String validateDocUpdate) {
        this.validateDocUpdate = validateDocUpdate;
    }

    /**
     * Set the array of URL rewriting rules set in the design document's {@code rewrites} property.
     *
     * @param rewrites array of JsonObjects each representing a rewrite rule
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#rewrite-rules"
     * target="_blank">Rewrite rules</a>
     */
    public void setRewrites(JsonArray rewrites) {
        this.rewrites = rewrites;
    }

    /**
     * @param fulltext JsonObject to set as the design document's {@code fulltext} property
     */
    public void setFulltext(JsonObject fulltext) {
        this.fulltext = fulltext;
    }

    /**
     * Set a JSON object defining the indexes of this design document.
     *
     * @param indexes JsonObject defining the indexes
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#indexes"
     * target="_blank">Indexes</a>
     */
    public void setIndexes(JsonObject indexes) {
        this.indexes = indexes;
    }

    /**
     * Define the changes feed filter functions set in this design document.
     *
     * @param filters map of filter name to function string
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#filter-functions"
     * target="_blank">Filter functions</a>
     */
    public void setFilters(Map<String, String> filters) {
        this.filters = filters;
    }

    /**
     * Set the show functions defined in this design document.
     *
     * @param shows map of show function name to function string
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#show-functions"
     * target="_blank">Show functions</a>
     */
    public void setShows(Map<String, String> shows) {
        this.shows = shows;
    }

    /**
     * Set the list functions defined in this design document.
     *
     * @param lists map of list function name to function string
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#list-functions"
     * target="_blank">List functions</a>
     */
    public void setLists(Map<String, String> lists) {
        this.lists = lists;
    }

    /**
     * Set the update handlers defined in this design document.
     *
     * @param updates map of update handler name to function string
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#update-handlers"
     * target="_blank">Update handlers</a>
     */
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

    /**
     * Compares this design document to the specified object. The result is {@code true} if and only
     * if the argument is not {@code null} and is a {@link DesignDocument} with the same
     * {@code _id}, {@code _rev}, and document contents.
     *
     * @param o The object to compare this design document to
     * @return {@code true} if the given object represents a design document equivalent to this
     * design document, {@code false} otherwise
     */
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
     * Encapsulates the Map-Reduce function of a view.
     *
     * <pre>
     * {@code
     *
     * // Javascript view definition in design document
     * "views": {
     *   "exampleView" : {
     *       "map" : "function(doc) { emit(doc.propertyA, doc.propertyB); }"
     *       "reduce" : "_count"
     *   }
     * }
     *
     * // Java example using MapReduce
     *
     * // Get the views from the design document
     * Map<String, MapReduce> views = ddoc.getViews();
     *
     * // Get the MapReduce for the "exampleView"
     * MapReduce exampleView = views.get("exampleView");
     *
     * // Get the strings of the map and reduce functions
     * exampleView.getMap(); // "function(doc) { emit(doc.propertyA, doc.propertyB); }"
     * exampleView.getReduce(); // "_count"
     * }
     * </pre>
     *
     * @author Ahmed Yehia
     */
    public static class MapReduce {
        private JsonElement map;
        private JsonElement reduce;
        private String dbcopy;

        public MapReduce() {
            super();
        }

        /**
         * Get the defined map function.
         *
         * @return string of the javascript map function
         */
        public String getMap() {
            return stringifyMRElement(map);
        }

        /**
         * Get the defined reduce function.
         *
         * @return string of the javascript reduce function
         */
        public String getReduce() {
            return stringifyMRElement(reduce);
        }

        /**
         * Set the map function.
         *
         * @param map string of the javascript map function
         */
        public void setMap(String map) {
            this.map = GSON.toJsonTree(map);
        }

        /**
         * Set the reduce function.
         *
         * @param reduce string of the javascript reduce function
         */
        public void setReduce(String reduce) {
            this.reduce = GSON.toJsonTree(reduce);
        }

        /**
         * <p>
         * Set the name of a database to copy the reduced view results into.
         * </p>
         * <p>
         * Use of the {@code dbcopy} feature is deprecated and is strongly discouraged.
         * For more information, see the <a target="_blank" href=
         * "https://console.bluemix.net/docs//services/Cloudant/release_info/deprecations.html#-dbcopy-">
         * Bluemix documentation</a> for this feature.
         * </p>
         *
         * @param databaseName of the database to store reduced view results in
         * @see #getDbCopy()
         */
        @Deprecated
        public void setDbCopy(String databaseName) {
            this.dbcopy = databaseName;
        }

        /**
         * @return the database name where reduced view results will be stored, or null if unset
         * @see #setDbCopy(String)
         */
        @Deprecated
        public String getDbCopy() {
            return dbcopy;
        }

        /**
         * @param element JSON object or string
         * @return a string form of the object or the JSON string with leading and trailing "
         * stripped
         */
        private String stringifyMRElement(JsonElement element) {
            if (element != null) {
                String stringifiedElement = element.toString();
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    // If the element is a JSON string, strip the leading and trailing "
                    return stringifiedElement.substring(1, stringifiedElement.length() - 1);
                } else {
                    return stringifiedElement;
                }
            } else {
                return null;
            }
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
