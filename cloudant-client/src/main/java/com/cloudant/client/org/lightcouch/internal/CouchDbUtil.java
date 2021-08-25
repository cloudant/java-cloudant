/*
 * Copyright (C) 2011 lightcouch.org
 * Copyright © 2015, 2021 IBM Corp. All rights reserved.
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

package com.cloudant.client.org.lightcouch.internal;

import static java.lang.String.format;

import com.cloudant.client.api.model.Permissions;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.Response;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provides various utility methods, for internal use.
 *
 * @author Ahmed Yehia
 */
final public class CouchDbUtil {

    public static final String DESIGN_PREFIX = "_design/";
    public static final String LOCAL_PREFIX = "_local/";


    private CouchDbUtil() {
        // Utility class
    }

    public static void assertNotEmpty(Object object, String prefix) throws
            IllegalArgumentException {
        assertNotNull(object, prefix);
        if (object instanceof String && ((String) object).length() == 0) {
            throw new IllegalArgumentException(format("%s may not be empty.", prefix));
        }
    }

    public static void assertNotNull(Object object, String prefix) throws IllegalArgumentException {
        if (object == null) {
            throw new IllegalArgumentException(format("%s may not be null.", prefix));
        }
    }

    public static void assertNull(Object object, String prefix) throws IllegalArgumentException {
        if (object != null) {
            throw new IllegalArgumentException(format("%s should be null.", prefix));
        }
    }

    /*
     * Throws an IllegalArgument exception if the ID is an _ prefixed name that isn't
     * either _design or _local.
     * 
     * @param id
     * @throws IllegalArgumentException
     */
    public static void assertDocumentTypeId(String id) throws IllegalArgumentException {
        boolean invalid = false;
        if (id.startsWith("_")) {
            if (id.startsWith(DESIGN_PREFIX) && !DESIGN_PREFIX.equals(id)) {
                invalid = false;
            } else if (id.startsWith(LOCAL_PREFIX) && !LOCAL_PREFIX.equals(id)) {
                invalid = false;
            } else {
                invalid = true;
            }
        
        }
        if (invalid) {
            throw new IllegalArgumentException(format("%s is not a valid document ID.", id));
        }
    }

    public static void assertValidAttachmentName(String attachmentName) throws IllegalArgumentException {
        if (attachmentName.startsWith("_")) {
            throw new IllegalArgumentException(format("%s is not a valid attachment name.", attachmentName));
        }
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // JSON

    public static <T> T jsonToObject(Gson gson, JsonElement elem, String key, Class<T> classType) {
        if(elem != null && !elem.isJsonNull()) {
            JsonElement keyElem = elem.getAsJsonObject().get(key);
            if(keyElem != null && !keyElem.isJsonNull()) {
                return gson.fromJson(elem.getAsJsonObject().get(key), classType);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * @return A JSON element as a String, or null if there is no member with that name or the
     * value was a JSON null.
     */
    public static String getAsString(JsonObject j, String e) {
        if (j != null && e != null) {
            JsonElement element = j.get(e);
            if (element != null && !element.isJsonNull()) {
                return element.getAsString();
            }
        }
        return null;
    }

    /**
     * @return A JSON element as <code>long</code>, or <code>0</code> if not found.
     */
    public static long getAsLong(JsonObject j, String e) {
        return (j.get(e) == null) ? 0L : j.get(e).getAsLong();
    }

    /**
     * @return A JSON element as <code>int</code>, or <code>0</code> if not found.
     */
    public static int getAsInt(JsonObject j, String e) {
        return (j.get(e) == null) ? 0 : j.get(e).getAsInt();
    }

    // Files

    public static String removeExtension(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    public static String streamToString(InputStream in) {
        try {
            return IOUtils.toString(in, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("Error reading stream.", e);
        } finally {
            close(in);
        }
    }

    /**
     * Closes the response input stream.
     *
     * @param response The {@link HttpConnection}
     */
    public static void close(InputStream response) {
        IOUtils.closeQuietly(response);
    }

    /**
     * Closes a resource.
     *
     * @param c The {@link Closeable} resource.
     */
    public static void close(Closeable c) {
        IOUtils.closeQuietly(c);
    }


    /**
     * @return A JSON element as a String, or null if not found, from the response
     */
    public static String getAsString(InputStream response, String e) {
        Reader reader = null;
        try {
            reader = new InputStreamReader(response, "UTF-8");
            return getAsString(new JsonParser().parse(reader).getAsJsonObject(), e);
        } catch (UnsupportedEncodingException e1) {
            // This should never happen as every implementation of the java platform is required
            // to support UTF-8.
            throw new RuntimeException(e1);
        } finally {
            close(reader);
            close(response);
        }
    }

    /**
     * create a HTTP POST request.
     *
     * @return {@link HttpConnection}
     */
    public static HttpConnection createPost(URI uri, String body, String contentType) {
        HttpConnection connection = Http.POST(uri, "application/json");
        if(body != null) {
            setEntity(connection, body, contentType);
        }
        return connection;
    }

    /**
     * Sets a JSON String as a request entity.
     *
     * @param connnection The request of {@link HttpConnection}
     * @param body        The JSON String to set.
     */
    public static void setEntity(HttpConnection connnection, String body, String contentType) {
        connnection.requestProperties.put("Content-type", contentType);
        connnection.setRequestBody(body);
    }

    /**
     * @param response The response from {@link com.cloudant.http.HttpConnection}
     * @return {@link Response}
     */
    public static <T> List<T> getResponseList(InputStream response, Gson gson,
                                              Type typeofT) throws CouchDbException {
        Reader reader = null;
        try {
            reader = new InputStreamReader(response, "UTF-8");
            return gson.fromJson(reader, typeofT);
        } catch (UnsupportedEncodingException e) {
            // This should never happen as every implementation of the java platform is required
            // to support UTF-8.
            throw new RuntimeException(e);
        }  finally {
            close(reader);
            close(response);
        }
    }

    /**
     * @param response The {@link InputStream} response of {@link HttpConnection}
     * @return {@link Response}
     */
    public static <T> T getResponse(InputStream response, Class<T> classType, Gson gson) throws
            CouchDbException {
        Reader reader = null;
        try {
            reader = new InputStreamReader(response, "UTF-8");
            return gson.fromJson(reader, classType);
        } catch (UnsupportedEncodingException e) {
            // This should never happen as every implementation of the java platform is required to support UTF-8.
            throw new RuntimeException(e);
        } finally {
            close(reader);
            close(response);
        }
    }

    /**
     * @param response The {@link InputStream} response of {@link HttpConnection}
     * @return {@link Response}
     */
    public static Map<String, EnumSet<Permissions>> getResponseMap(InputStream response, Gson
            gson, Type typeofT) throws CouchDbException {
        Reader reader = null;
        try {
            reader = new InputStreamReader(response, "UTF-8");
            return gson.fromJson(reader, typeofT);
        } catch (UnsupportedEncodingException e) {
            // This should never happen as every implementation of the java platform is required to support UTF-8.
            throw new RuntimeException(e);
        } finally {
            close(reader);
            close(response);
        }
    }
}
