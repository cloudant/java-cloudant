package com.cloudant.client.internal;

import java.net.URI;

/**
 * Subclass for building a Cloudant account URI.
 * This class provides fluent style setters to create and build a URI.
 * If a database URI is required, see {@link DatabaseURIHelper}.
 */
public class URIBase extends URIBaseMethods<URIBase> {

    public URIBase(URI baseUri) {
        this.baseUri = baseUri;
    }

    @Override
    public URIBase returnThis() {
        return this;
    }
}
