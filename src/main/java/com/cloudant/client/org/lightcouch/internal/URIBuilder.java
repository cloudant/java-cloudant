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

package com.cloudant.client.org.lightcouch.internal;

import com.cloudant.client.org.lightcouch.Params;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

/**
 * Helper class for construction of HTTP request URIs.
 *
 * @author Ahmed Yehia
 * @since 0.0.2
 */
public class URIBuilder {
    private String scheme;
    private String host;
    private int port;
    private String path = "";
    private String encodedPath = "";
    /* The final query */
    private final StringBuilder query = new StringBuilder();
    /* key=value params */
    private final Params qParams = new Params();
    private String userInfo = "";

    public static URIBuilder buildUri() {
        return new URIBuilder();
    }

    public static URIBuilder buildUri(URI uri) {
        URIBuilder builder;
        if(uri.getUserInfo() != null && !uri.getUserInfo().isEmpty()) {
            builder = URIBuilder.buildUri().scheme(uri.getScheme())
                    .userInfo(uri.getUserInfo()).host(uri.getHost())
                    .port(uri.getPort()).path(uri.getPath());
        } else {
            builder = URIBuilder.buildUri().scheme(uri.getScheme())
                    .host(uri.getHost()).port(uri.getPort()).path(uri.getPath());
        }
        return builder;
    }


    public URI build() {
        try {
            StringBuilder queryBuilder;
            if (query.length() > 0) {
                // This will only happen if the deprecated query(String) method was used.
                queryBuilder = new StringBuilder(encodeQuery(query.toString()));
            } else {
                queryBuilder = new StringBuilder();
            }

            for (int i = 0; i < qParams.size(); i++) {
                String amp = (i != qParams.size() - 1) ? "&" : "";
                queryBuilder.append(qParams.get(i).toURLEncodedString() + amp);
            }
            String q = (queryBuilder.length() == 0) ? "" : "?" + queryBuilder.toString();
            String uriString;
            if(userInfo != null && !userInfo.isEmpty()) {
                uriString = String.format("%s://%s@%s:%s%s%s", scheme, userInfo, host, port,
                        path, q);
            } else {
                uriString = String.format("%s://%s:%s%s%s", scheme, host, port, path, q);
            }
            return new URI(uriString);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public URI buildEncoded() {
        for (int i = 0; i < qParams.size(); i++) {
            String amp = (i != qParams.size() - 1) ? "&" : "";
            query.append(qParams.get(i).toURLEncodedString() + amp);
        }
        try {
            String q = (query.length() == 0) ? "" : "?" + query;
            String uri = "";
            if(userInfo != null) {
                uri = String.format("%s://%s@%s:%s%s%s%s", scheme, userInfo, host, port, path,
                        encodedPath, q);
            } else {
                uri = String.format("%s://%s:%s%s%s%s", scheme, host, port, path, encodedPath,
                        q);
            }
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public URIBuilder scheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    public URIBuilder host(String host) {
        this.host = host;
        return this;
    }

    public URIBuilder port(int port) {
        this.port = port;
        return this;
    }

    public URIBuilder path(String path) {
        this.path += path;
        return this;
    }

    public URIBuilder userInfo(String userInfo) {
        this.userInfo += userInfo;
        return this;
    }

    public URIBuilder pathToEncode(String path) {
        try {
            encodedPath = URLEncoder.encode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
        return this;
    }

    /**
     * Add the given {@code name} and {@code value} to the query parameters or replace
     * the existing query parameter matching {@code name} with one with the new {@code value}.
     *
     * @param name    The name of the parameter to add/replace.
     * @param value   The value of the parameter.
     * @param replace set to true to replace the value of an existing query parameter matching
     *                {@code name}, or false to add a new one. Note that if this is true and there
     *                is no parameter matching {@code name}, the parameter will be added.
     * @return The updated {@link URIBuilder} object.
     */
    public URIBuilder query(String name, Object value, boolean replace) {
        if (name != null && value != null) {
            if (replace) {
                this.qParams.replaceOrAdd(name, value.toString());
            } else {
                this.qParams.addParam(name, value.toString());
            }
        }
        return this;

    }

    /**
     * Add the given {@code name} and {@code value} to the query parameters.
     *
     * @param name  The name to add.
     * @param value The value to add.
     * @return The updated {@link URIBuilder} object.
     */
    public URIBuilder query(String name, Object value) {
        return query(name, value, true);

    }

    /**
     * @deprecated Use {@link #query(String, Object)} instead.
     */
    @Deprecated
    public URIBuilder query(String query) {
        if (query != null) {
            this.query.append(query);
        }
        return this;
    }

    public URIBuilder query(Params params) {
        if (params.getParams() != null) {
            this.qParams.addAll(params);
        }
        return this;
    }

    private String encodeQuery(String in) {
        try {
            URI uri = new URI(
                    null, // scheme
                    null, // authority
                    null, // path
                    in,   // query
                    null  // fragment
            );
            return uri.toASCIIString()
                    .substring(1); // remove leading ?;
        } catch (URISyntaxException e) {
            throw new RuntimeException(
                    "Couldn't encode query parameter " + in,
                    e);
        }
    }
}
