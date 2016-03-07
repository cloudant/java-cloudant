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

package com.cloudant.client.internal.views;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * This class is purely for serializing the fields necessary to generate an opaque pagination token.
 */
class PaginationToken {

    @SerializedName("d")
    public Boolean descending = null;

    @SerializedName("e")
    public JsonElement endkey = null;

    @SerializedName("ei")
    public String endkey_docid = null;

    @SerializedName("i")
    public Boolean inclusive_end = null;

    @SerializedName("s")
    public JsonElement startkey = null;

    @SerializedName("si")
    public String startkey_docid = null;

    @SerializedName("n")
    long pageNumber;

    @SerializedName("dr")
    PageMetadata.PagingDirection direction;

    // Construct a pagination token using the appropriate metadata
    private PaginationToken(PageMetadata pageMetadata) {
        this.pageNumber = pageMetadata.pageNumber;
        this.direction = pageMetadata.direction;
        this.descending = pageMetadata.pageRequestParameters.descending;
        this.endkey = pageMetadata.pageRequestParameters.endkey;
        this.endkey_docid = pageMetadata.pageRequestParameters.endkey_docid;
        this.inclusive_end = pageMetadata.pageRequestParameters.inclusive_end;
        this.startkey = pageMetadata.pageRequestParameters.startkey;
        this.startkey_docid = pageMetadata.pageRequestParameters.startkey_docid;
    }

    /**
     * Generate a PageMetadata object for the page represented by the specified pagination token.
     *
     * @param paginationToken   opaque pagination token
     * @param initialParameters the initial view query parameters (i.e. for the page 1 request).
     * @param <K>               the view key type
     * @param <V>               the view value type
     * @return PageMetadata object for the given page
     */
    static <K, V> PageMetadata<K, V> mergeTokenAndQueryParameters(String paginationToken, final
    ViewQueryParameters<K, V> initialParameters) {

        // Decode the base64 token into JSON
        String json = new String(Base64.decodeBase64(paginationToken), Charset.forName("UTF-8"));

        // Get a suitable Gson, we need any adapter registered for the K key type
        Gson paginationTokenGson = getGsonWithKeyAdapter(initialParameters);

        // Deserialize the pagination token JSON, using the appropriate K, V types
        PaginationToken token = paginationTokenGson.fromJson(json, PaginationToken.class);

        // Create new query parameters using the initial ViewQueryParameters as a starting point.
        ViewQueryParameters<K, V> tokenPageParameters = initialParameters.copy();

        // Merge the values from the token into the new query parameters
        tokenPageParameters.descending = token.descending;
        tokenPageParameters.endkey = token.endkey;
        tokenPageParameters.endkey_docid = token.endkey_docid;
        tokenPageParameters.inclusive_end = token.inclusive_end;
        tokenPageParameters.startkey = token.startkey;
        tokenPageParameters.startkey_docid = token.startkey_docid;

        return new PageMetadata<K, V>(token.direction, token
                .pageNumber, tokenPageParameters);
    }

    /**
     * Generate an opaque pagination token from the supplied PageMetadata.
     *
     * @param pageMetadata page metadata of the page for which the token should be generated
     * @return opaque pagination token
     */
    static String tokenize(PageMetadata<?, ?> pageMetadata) {
        try {
            Gson g = getGsonWithKeyAdapter(pageMetadata.pageRequestParameters);
            return new String(Base64.encodeBase64URLSafe(g.toJson(new PaginationToken
                    (pageMetadata)).getBytes("UTF-8")),
                    Charset.forName("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            //all JVMs should support UTF-8
            throw new RuntimeException(e);
        }
    }

    private static <K, V> Gson getGsonWithKeyAdapter(ViewQueryParameters<K, V> vqp) {
        return new GsonBuilder().registerTypeAdapter(vqp.getKeyType(), vqp.getClient().getGson()
                .getAdapter(vqp.getKeyType())).create();
    }
}
