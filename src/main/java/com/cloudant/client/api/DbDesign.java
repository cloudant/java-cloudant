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

package com.cloudant.client.api;

import static java.lang.String.format;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.assertNotEmpty;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.listResources;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.readFile;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.removeExtension;
import static com.cloudant.client.org.lightcouch.internal.URIBuilder.buildUri;

import com.cloudant.client.api.model.DesignDocument;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.cloudant.client.org.lightcouch.CouchDatabase;
import com.cloudant.client.org.lightcouch.CouchDatabaseBase;
import com.cloudant.client.org.lightcouch.CouchDbDesign;
import com.cloudant.client.api.model.DesignDocument.MapReduce;
import com.cloudant.client.org.lightcouch.Response;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import DesignDocument;

public class DbDesign {

    private static final String DESIGN_DOCS_DIR = "design-docs";
    private static final String JAVASCRIPT = "javascript";
    private static final String DESIGN_PREFIX = "_design/";
    private static final String VALIDATE_DOC = "validate_doc_update";
    private static final String VIEWS = "views";
    private static final String FILTERS = "filters";
    private static final String SHOWS = "shows";
    private static final String LISTS = "lists";
    private static final String UPDATES = "updates";
    private static final String REWRITES = "rewrites";
    private static final String FULLTEXT = "fulltext";
    private static final String INDEXES = "indexes";
    private static final String MAP_JS = "map.js";
    private static final String REDUCE_JS = "reduce.js";


    private CouchDbDesign couchDbDesign;
    private URI dbUri;
    private CouchDatabaseBase dbc;
    private CloudantClient client;

    DbDesign(CouchDatabase db, CloudantClient client) {
        this.couchDbDesign = db.design();
        this.dbUri = db.getDBUri();
        this.dbc = db;
        this.client = client;
    }


    /**
     * Synchronizes a design document to the Database.
     * <p>This method will first try to find a document in the database with the same id
     * as the given document, if it is not found then the given document will be saved to the
     * database.
     * <p>If the document was found in the database, it will be compared with the given document
     * using
     * {@code equals()}. If both documents are not equal, then the given document will be saved to
     * the
     * database and updates the existing document.
     *
     * @param document The design document to synchronize
     * @return {@link Response} as a result of a document save or update, or returns {@code null}
     * if no
     * action was taken and the document in the database is up-to-date with the given document.
     */
    public Response synchronizeWithDb(DesignDocument document) {
        return couchDbDesign.synchronizeWithDb(document);
    }

    /**
     * Synchronize all design documents on desk to the database.
     *
     * @see DbDesign#synchronizeWithDb(DesignDocument)
     * @see CouchDbDesign#synchronizeAllWithDb()
     */
    public void synchronizeAllWithDb() {
        couchDbDesign.synchronizeAllWithDb();
    }

    /**
     * Gets a design document from the database.
     *
     * @param id The document id
     * @return {@link DesignDocument}
     */
    public DesignDocument getFromDb(String id) {
        final URI uri = buildUri(dbUri).path(id).build();
        return client.couchDbClient.get(uri, DesignDocument.class);
    }

    /**
     * Gets a design document from the database.
     *
     * @param id  The document id
     * @param rev The document revision
     * @return {@link DesignDocument}
     */
    public DesignDocument getFromDb(String id, String rev) {
        assertNotEmpty(id, "id");
        assertNotEmpty(id, "rev");
        final URI uri = buildUri(dbc.getDBUri()).path(id).query("rev", rev).build();
        return client.couchDbClient.get(uri, DesignDocument.class);
    }

    /**
     * Gets all design documents from desk.
     *
     * @see #getFromDesk(String)
     */
    public List<DesignDocument> getAllFromDesk() {
        final List<DesignDocument> designDocsList = new ArrayList<DesignDocument>();
        for (String docName : listResources(format("%s/", "design-docs"))) {
            designDocsList.add(getFromDesk(docName));
        }
        return designDocsList;
    }

    /**
     * Gets a design document from desk.
     *
     * @param id The document id to get.
     * @return {@link DesignDocument}
     */
    public DesignDocument getFromDesk(String id) {
        assertNotEmpty(id, "id");
        final DesignDocument dd = new DesignDocument();
        final String rootPath = format("%s/%s/", DESIGN_DOCS_DIR, id);
        final List<String> elements = listResources(rootPath);
        if (elements == null) {
            throw new IllegalArgumentException("Design docs directory cannot be empty.");
        }
        // Views
        Map<String, MapReduce> views = null;
        if (elements.contains(VIEWS)) {
            views = new HashMap<String, MapReduce>();
            final String viewsPath = format("%s%s/", rootPath, VIEWS);
            for (String viewDirName : listResources(viewsPath)) { // views sub-dirs
                final MapReduce mr = new MapReduce();
                final String viewPath = format("%s%s/", viewsPath, viewDirName);
                final List<String> dirList = listResources(viewPath);
                for (String fileName : dirList) { // view files
                    final String def = readFile(format("/%s%s", viewPath, fileName));
                    if (MAP_JS.equals(fileName)) {
                        mr.setMap(def);
                    } else if (REDUCE_JS.equals(fileName)) {
                        mr.setReduce(def);
                    }
                } // /foreach view files
                views.put(viewDirName, mr);
            } // /foreach views sub-dirs
        } // /views
        dd.setId(DESIGN_PREFIX + id);
        dd.setLanguage(JAVASCRIPT);
        dd.setViews(views);
        dd.setFilters(populateMap(rootPath, elements, FILTERS));
        dd.setShows(populateMap(rootPath, elements, SHOWS));
        dd.setLists(populateMap(rootPath, elements, LISTS));
        dd.setUpdates(populateMap(rootPath, elements, UPDATES));
        dd.setValidateDocUpdate(readContent(elements, rootPath, VALIDATE_DOC));
        dd.setRewrites(client.getGson().fromJson(readContent(elements, rootPath, REWRITES),
                JsonArray.class));
        dd.setFulltext(client.getGson().fromJson(readContent(elements, rootPath, FULLTEXT),
                JsonObject.class));
        dd.setIndexes(client.getGson().fromJson(readContent(elements, rootPath, INDEXES),
                JsonObject.class));
        return dd;
    }

    public String readContent(List<String> elements, String rootPath,
                              String element) {
        return couchDbDesign.readContent(elements, rootPath, element);
    }

    private Map<String, String> populateMap(String rootPath, List<String> elements, String element) {
        Map<String, String> functionsMap = null;
        if (elements.contains(element)) {
            functionsMap = new HashMap<String, String>();
            String path = format("%s%s/", rootPath, element);
            for (String fileName : listResources(path)) {
                String contents = readFile(format("/%s%s", path, fileName));
                functionsMap.put(removeExtension(fileName), contents);
            }
        }
        return functionsMap;
    }

}
