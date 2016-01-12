/*
 * Copyright (c) 2016 IBM Corp. All rights reserved.
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

import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.assertNotEmpty;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.close;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.createPost;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.getAsString;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.getResponse;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.getResponseList;

import com.cloudant.client.api.model.DbInfo;
import com.cloudant.client.api.model.FindByIndexOptions;
import com.cloudant.client.api.model.Index;
import com.cloudant.client.api.model.IndexField;
import com.cloudant.client.api.model.Params;
import com.cloudant.client.api.model.Permissions;
import com.cloudant.client.api.model.Shard;
import com.cloudant.client.api.views.AllDocsRequestBuilder;
import com.cloudant.client.api.views.ViewRequestBuilder;
import com.cloudant.client.internal.DatabaseURIHelper;
import com.cloudant.client.internal.URIBase;
import com.cloudant.client.internal.views.AllDocsRequestBuilderImpl;
import com.cloudant.client.internal.views.AllDocsRequestResponse;
import com.cloudant.client.internal.views.ViewQueryParameters;
import com.cloudant.client.org.lightcouch.Changes;
import com.cloudant.client.org.lightcouch.CouchDatabase;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.cloudant.client.org.lightcouch.Response;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Contains a Database Public API implementation.
 *
 * Methods may throw a {@link NoDocumentException} if the database does not exist.
 *
 * @author Mario Briggs
 * @since 0.0.1
 */
class DatabaseImpl implements Database {

    static final Logger log = Logger.getLogger(DatabaseImpl.class.getCanonicalName());
    private CouchDatabase db;
    private CloudantClient client;
    private final URI apiV2DBSecurityURI;

    /**
     * @param client
     * @param db
     */
    DatabaseImpl(CloudantClient client, CouchDatabase db) {
        super();
        this.client = client;
        this.db = db;
        apiV2DBSecurityURI = new URIBase(client.getBaseUri()).path("_api").path("v2").path("db")
                .path(db.getDbName()).path("_security").build();
    }

    @Override
    public void setPermissions(String userNameorApikey, EnumSet<Permissions> permissions) {
        assertNotEmpty(userNameorApikey, "userNameorApikey");
        assertNotEmpty(permissions, "permissions");
        final JsonArray jsonPermissions = new JsonArray();
        for (Permissions s : permissions) {
            final JsonPrimitive permission = new JsonPrimitive(s.toString());
            jsonPermissions.add(permission);
        }

        // get existing permissions
        JsonObject perms = getPermissionsObject();

        // now set back
        JsonElement elem = perms.get("cloudant");
        if (elem == null) {
            perms.addProperty("_id", "_security");
            elem = new JsonObject();
            perms.add("cloudant", elem);
        }
        elem.getAsJsonObject().add(userNameorApikey, jsonPermissions);

        InputStream response = null;
        HttpConnection put = Http.PUT(apiV2DBSecurityURI, "application/json");
        put.setRequestBody(client.getGson().toJson(perms));
        try {
            response = client.couchDbClient.executeToInputStream(put);
            String ok = getAsString(response, "ok");
            if (!ok.equalsIgnoreCase("true")) {
                //raise exception
            }
        } finally {
            close(response);
        }
    }

    /**
     * @return /api/v2/db/$dbname/_security JSON data
     * @throws UnsupportedOperationException if called on a database that does not provide the
     *                                       Cloudant authorization API
     */
    private JsonObject getPermissionsObject() {
        try {
            return client.couchDbClient.get(apiV2DBSecurityURI, JsonObject.class);
        } catch (CouchDbException exception) {
            //currently we can't inspect the HttpResponse code
            //being in this catch block means it was not a 20x code
            //look for the "bad request" that implies the endpoint is not supported
            if (exception.getMessage().toLowerCase().contains("bad request")) {
                throw new UnsupportedOperationException("The methods getPermissions and " +
                        "setPermissions are not supported for this database, consider using the " +
                        "/db/_security endpoint.");
            } else {
                throw exception;
            }
        }
    }

    @Override
    public Map<String, EnumSet<Permissions>> getPermissions() {
        JsonObject perms = getPermissionsObject();
        return client.getGson().getAdapter(new TypeToken<Map<String, EnumSet<Permissions>>>() {
        }).fromJsonTree(perms);
    }

    @Override
    public List<Shard> getShards() {
        InputStream response = null;
        try {
            response = client.couchDbClient.get(new DatabaseURIHelper(db.getDBUri()).path("_shards")
                    .build());
            return getResponseList(response, client.getGson(),
                    new TypeToken<List<Shard>>() {
                    }.getType());
        } finally {
            close(response);
        }
    }

    @Override
    public Shard getShard(String docId) {
        assertNotEmpty(docId, "docId");
        return client.couchDbClient.get(new DatabaseURIHelper(db.getDBUri()).path("_shards")
                        .path(docId).build(),
                Shard.class);
    }

    @Override
    public void createIndex(String indexName, String designDocName, String indexType,
                            IndexField[] fields) {
        JsonObject indexDefn = getIndexDefinition(indexName, designDocName, indexType, fields);
        createIndex(indexDefn.toString());
    }

    @Override
    public void createIndex(String indexDefinition) {
        assertNotEmpty(indexDefinition, "indexDefinition");
        InputStream putresp = null;
        URI uri = new DatabaseURIHelper(db.getDBUri()).path("_index").build();
        try {
            putresp = client.couchDbClient.executeToInputStream(createPost(uri, indexDefinition,
                    "application/json"));
            String result = getAsString(putresp, "result");
            if (result.equalsIgnoreCase("created")) {
                log.info(String.format("Created Index: '%s'", indexDefinition));
            } else {
                log.warning(String.format("Index already exists : '%s'", indexDefinition));
            }
        } finally {
            close(putresp);
        }
    }

    @Override
    public <T> List<T> findByIndex(String selectorJson, Class<T> classOfT) {
        return findByIndex(selectorJson, classOfT, new FindByIndexOptions());
    }

    @Override
    public <T> List<T> findByIndex(String selectorJson, Class<T> classOfT, FindByIndexOptions
            options) {
        assertNotEmpty(selectorJson, "selectorJson");
        assertNotEmpty(options, "options");

        URI uri = new DatabaseURIHelper(db.getDBUri()).path("_find").build();
        JsonObject body = getFindByIndexBody(selectorJson, options);
        InputStream stream = null;
        try {
            stream = client.couchDbClient.executeToInputStream(createPost(uri, body.toString(),
                    "application/json"));
            Reader reader = new InputStreamReader(stream, "UTF-8");
            JsonArray jsonArray = new JsonParser().parse(reader)
                    .getAsJsonObject().getAsJsonArray("docs");
            List<T> list = new ArrayList<T>();
            for (JsonElement jsonElem : jsonArray) {
                JsonElement elem = jsonElem.getAsJsonObject();
                T t = client.getGson().fromJson(elem, classOfT);
                list.add(t);
            }
            return list;
        } catch (UnsupportedEncodingException e) {
            // This should never happen as every implementation of the java platform is required
            // to support UTF-8.
            throw new RuntimeException(e);
        } finally {
            close(stream);
        }
    }

    @Override
    public List<Index> listIndices() {
        InputStream response = null;
        try {
            URI uri = new DatabaseURIHelper(db.getDBUri()).path("_index").build();
            response = client.couchDbClient.get(uri);
            return getResponseList(response, client.getGson(),
                    new TypeToken<List<Index>>() {
                    }.getType());
        } finally {
            close(response);
        }
    }

    @Override
    public void deleteIndex(String indexName, String designDocId) {
        assertNotEmpty(indexName, "indexName");
        assertNotEmpty(designDocId, "designDocId");
        URI uri = new DatabaseURIHelper(db.getDBUri()).path("_index").path(designDocId)
                .path("json").path(indexName).build();
        InputStream response = null;
        try {
            HttpConnection connection = Http.DELETE(uri);
            response = client.couchDbClient.executeToInputStream(connection);
            getResponse(response, Response.class, client.getGson());
        } finally {
            close(response);
        }
    }

    @Override
    public Search search(String searchIndexId) {
        return new Search(client, this, searchIndexId);
    }

    @Override
    public DesignDocumentManager getDesignDocumentManager() {
        return new DesignDocumentManager(this);
    }

    @Override
    public ViewRequestBuilder getViewRequestBuilder(String designDoc, String viewName) {
        return new ViewRequestBuilder(client, this, designDoc, viewName);
    }

    @Override
    public AllDocsRequestBuilder getAllDocsRequestBuilder() {
        return new AllDocsRequestBuilderImpl(new ViewQueryParameters<String,
                AllDocsRequestResponse.Revision>(client, this, "", "", String.class,
                AllDocsRequestResponse.Revision.class) {
            protected DatabaseURIHelper getViewURIBuilder() {
                return new DatabaseURIHelper(db.getDBUri()).path("_all_docs");
            }
        });
    }

    @Override
    public com.cloudant.client.api.Changes changes() {
        Changes couchDbChanges = db.changes();
        com.cloudant.client.api.Changes changes = new com.cloudant.client.api.Changes
                (couchDbChanges);
        return changes;
    }

    @Override
    public <T> T find(Class<T> classType, String id) {
        return db.find(classType, id);
    }

    @Override
    public <T> T find(Class<T> classType, String id, Params params) {
        assertNotEmpty(params, "params");
        return db.find(classType, id, params.getInternalParams());
    }

    @Override
    public <T> T find(Class<T> classType, String id, String rev) {
        return db.find(classType, id, rev);
    }

    @Override
    public <T> T findAny(Class<T> classType, String uri) {
        return db.findAny(classType, uri);
    }

    @Override
    public InputStream find(String id) {
        return db.find(id);
    }

    @Override
    public InputStream find(String id, String rev) {
        return db.find(id, rev);
    }

    @Override
    public boolean contains(String id) {
        return db.contains(id);
    }

    @Override
    public com.cloudant.client.api.model.Response save(Object object) {
        Response couchDbResponse = db.save(object);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

    @Override
    public com.cloudant.client.api.model.Response save(Object object, int writeQuorum) {
        Response couchDbResponse = client.couchDbClient.put(getDBUri(), object, true, writeQuorum);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

    @Override
    public com.cloudant.client.api.model.Response post(Object object) {
        Response couchDbResponse = db.post(object);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

    @Override
    public com.cloudant.client.api.model.Response post(Object object, int writeQuorum) {
        assertNotEmpty(object, "object");
        InputStream response = null;
        try {
            URI uri = new DatabaseURIHelper(db.getDBUri()).query("w", writeQuorum).build();
            response = client.couchDbClient.executeToInputStream(createPost(uri, client.getGson()
                            .toJson(object),
                    "application/json"));
            Response couchDbResponse = getResponse(response, Response.class, client.getGson());
            com.cloudant.client.api.model.Response cloudantResponse = new com.cloudant.client.api
                    .model.Response(couchDbResponse);
            return cloudantResponse;
        } finally {
            close(response);
        }
    }

    @Override
    public com.cloudant.client.api.model.Response update(Object object) {
        Response couchDbResponse = db.update(object);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

    @Override
    public com.cloudant.client.api.model.Response update(Object object, int writeQuorum) {
        Response couchDbResponse = client.couchDbClient.put(getDBUri(), object, false, writeQuorum);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

    @Override
    public com.cloudant.client.api.model.Response remove(Object object) {
        Response couchDbResponse = db.remove(object);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

    @Override
    public com.cloudant.client.api.model.Response remove(String id, String rev) {
        Response couchDbResponse = db.remove(id, rev);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

    @Override
    public List<com.cloudant.client.api.model.Response> bulk(List<?> objects) {
        List<Response> couchDbResponseList = db.bulk(objects, false);
        List<com.cloudant.client.api.model.Response> cloudantResponseList = new ArrayList<com
                .cloudant.client.api.model.Response>();
        for (Response couchDbResponse : couchDbResponseList) {
            com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                    .Response(couchDbResponse);
            cloudantResponseList.add(response);
        }
        return cloudantResponseList;
    }

    @Override
    public com.cloudant.client.api.model.Response saveAttachment(InputStream in, String name,
                                                                 String contentType) {
        Response couchDbResponse = db.saveAttachment(in, name, contentType);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

    @Override
    public com.cloudant.client.api.model.Response saveAttachment(InputStream in, String name,
                                                                 String contentType, String
                                                                         docId, String docRev) {
        Response couchDbResponse = db.saveAttachment(in, name, contentType, docId, docRev);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

    @Override
    public String invokeUpdateHandler(String updateHandlerUri, String docId,
                                      Params params) {
        assertNotEmpty(params, "params");
        return db.invokeUpdateHandler(updateHandlerUri, docId, params.getInternalParams());
    }

    @Override
    public URI getDBUri() {
        return db.getDBUri();
    }

    @Override
    public DbInfo info() {
        return client.couchDbClient.get(new DatabaseURIHelper(db.getDBUri()).getDatabaseUri(),
                DbInfo.class);
    }

    @Override
    public void ensureFullCommit() {
        db.ensureFullCommit();
    }

    // private helper methods

    /**
     * Form a create index json from parameters
     */
    private JsonObject getIndexDefinition(String indexName, String designDocName,
                                          String indexType, IndexField[] fields) {
        assertNotEmpty(fields, "index fields");
        JsonObject indexObject = new JsonObject();
        if (!(indexName == null || indexName.isEmpty())) {
            indexObject.addProperty("name", indexName);
        }
        if (!(designDocName == null || designDocName.isEmpty())) {
            indexObject.addProperty("ddoc", designDocName);
        }
        if (!(indexType == null || indexType.isEmpty())) {

            indexObject.addProperty("type", indexType);
        }

        JsonArray fieldsArray = new JsonArray();
        for (int i = 0; i < fields.length; i++) {
            JsonObject fieldObject = new JsonObject();
            fieldObject.addProperty(fields[i].getName(), fields[i].getOrder().toString());
            fieldsArray.add(fieldObject);
        }
        JsonObject arrayOfFields = new JsonObject();
        arrayOfFields.add("fields", fieldsArray);
        indexObject.add("index", arrayOfFields);

        return indexObject;
    }

    private JsonObject getFindByIndexBody(String selectorJson,
                                          FindByIndexOptions options) {

        JsonArray fieldsArray = new JsonArray();
        if (options.getFields().size() > 0) {
            for(String field : options.getFields()) {
                JsonPrimitive jsonField = client.getGson().fromJson(field,
                        JsonPrimitive.class);
                fieldsArray.add(jsonField);
            }
        }

        JsonArray sortArray = new JsonArray();
        if (options.getSort().size() > 0) {

            for(IndexField sort : options.getSort()) {
                JsonObject sortObject = new JsonObject();
                sortObject.addProperty(sort.getName(), sort.getOrder().toString());
                sortArray.add(sortObject);
            }
        }

        JsonObject indexObject = new JsonObject();

        //parse and find if valid json issue #28
        JsonObject selectorObject = null;
        boolean isObject = true;
        try {
            selectorObject = getGson().fromJson(selectorJson, JsonObject.class);
        } catch (JsonParseException e) {
            isObject = false;
        }

        if(!isObject) {
            if(selectorJson.startsWith("\"selector\"")) {
                selectorJson = selectorJson.substring(selectorJson.indexOf(":") + 1,
                        selectorJson.length()).trim();
                selectorObject = getGson().fromJson(selectorJson, JsonObject.class);
            } else {
                throw new JsonParseException("selectorJson should be valid json or like " +
                        "\"selector\": {...} ");
            }
        }

        if(selectorObject.has("selector")) {
            indexObject.add("selector", selectorObject.get("selector"));
        } else {
            indexObject.add("selector", selectorObject);
        }

        if(fieldsArray.size() > 0) {
            indexObject.add("fields", fieldsArray);
        }
        if(sortArray.size() > 0) {
            indexObject.add("sort", sortArray);
        }
        if (options.getLimit() != null) {
            indexObject.addProperty("limit", options.getLimit());
        }
        if (options.getSkip() != null) {
            indexObject.addProperty("skip", options.getSkip());
        }
        if (options.getReadQuorum() != null) {
            indexObject.addProperty("r", options.getReadQuorum());
        }
        if (options.getUseIndex() != null) {
            indexObject.add("use_index", getGson().fromJson(options.getUseIndex(),
                    JsonArray.class));
        }

        return indexObject;
    }

    Gson getGson() {
        return client.getGson();
    }
}


