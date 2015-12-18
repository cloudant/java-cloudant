package com.cloudant.client.internal;

import com.cloudant.client.org.lightcouch.Params;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Provides methods using fluent style setters to build a Cloudant account or database URI.
 * The use of generics provides the subclasses a way to re-use common URI methods with
 * different types. Current types are {@link DatabaseURIHelper} for creating a URI to
 * interact with a Cloudant database, and {@link URIBase} for interacting with a
 * Cloudant account.
 */
abstract class URIBaseMethods<T extends URIBaseMethods> {

    private static final String _design_prefix_encoded = "_design%2F";
    private static final String _local_prefix_encoded = "_local%2F";
    /* key=value params */
    Params qParams = new Params();
    String completeQuery = "";
    String path = "";
    URI baseUri;

    public abstract T returnThis();

    /**
     * Sets a path(s) for the URI.
     * Note: File separator is handled in this method
     * and should not be passed as parameter.
     *
     * @return The updated {@link T} object.
     */
    public T path(String path) {
        if (path.length() == 0) {
            this.path += encodePath(path);
        } else {
            this.path += "/" + encodePath(path);
        }
        return returnThis();
    }

    /**
     * Add the given {@code name} and {@code value} to the query parameters.
     *
     * @param name    The name of the parameter to add/replace.
     * @param value   The value of the parameter.
     * @return The updated {@link T} object.
     */
    public T query(String name, Object value) {
        this.query(name, value, true);
        return returnThis();
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
     * @return The updated {@link T} object.
     */
    public T query(String name, Object value, boolean replace) {
        if (name != null && value != null) {
            if (replace) {
                this.qParams.replaceOrAdd(name, value.toString());
            } else {
                this.qParams.addParam(name, value.toString());
            }
        }
        return returnThis();
    }

    /**
     * Return the Cloudant account URI.
     * @return account URI
     */
    public URI getUri() {
        return baseUri;
    }

    /**
     * Encode a path in a manner suitable for a GET request
     * @param in The path to encode, eg "a/document"
     * @return The encoded path eg "a%2Fdocument"
     */
    String encodePath(String in) {
        try {
            String encodedString = HierarchicalUriComponents.encodeUriComponent(in, "UTF-8",
                    HierarchicalUriComponents.Type.PATH_SEGMENT);
            if (encodedString.startsWith(_design_prefix_encoded) ||
                    encodedString.startsWith(_local_prefix_encoded)) {
                // we replaced the first slash in the design or local doc URL, which we shouldn't
                // so let's put it back
                return encodedString.replaceFirst("%2F", "/");
            } else {
                return encodedString;
            }
        } catch (UnsupportedEncodingException uee) {
            // This should never happen as every implementation of the java platform is required
            // to support UTF-8.
            throw new RuntimeException(
                    "Couldn't encode ID " + in,
                    uee);
        }
    }

    /**
     * Build and return the complete URI containing values
     * such as the document ID, attachment ID, and query syntax.
     */
    public URI build() {
        try {
            String uriString = String.format("%s%s", baseUri.toASCIIString(),
                    (path.isEmpty() ? "" : path));
            if(qParams != null && qParams.size() > 0) {
                //Add queries together if both exist
                if(!completeQuery.isEmpty()) {
                    uriString = String.format("%s?%s&%s", uriString,
                            getJoinedQuery(qParams.getParams()),
                            completeQuery);
                } else {
                    uriString = String.format("%s?%s", uriString,
                            getJoinedQuery(qParams.getParams()));
                }
            } else if(!completeQuery.isEmpty()) {
                uriString = String.format("%s?%s", uriString, completeQuery);
            }

            return new URI(uriString);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String getJoinedQuery(List<String> parts) {
        StringBuilder queryBuilder = new StringBuilder();
        for(String query : parts) {
            if(query != null && !query.isEmpty()) {
                queryBuilder.append(query);
                queryBuilder.append("&");
            }
        }
        //Remove final `&`
        queryBuilder.deleteCharAt(queryBuilder.length() - 1);
        return queryBuilder.toString();
    }
}
