package org.lightcouch;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;

public class Param {

    private String key;
    private Object value;

    public Param(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String toURLEncodedString() {
        return String.format("%s=%s", encodeQueryParameter(getKey()), encodeQueryParameter(getValue().toString()));
    }

    private static String encodeQueryParameter(String in) {
        // As this is to escape individual parameters, we need to
        // escape &, = and +.
        try {
            URI uri = new URI(
                    null, // scheme
                    null, // authority
                    null, // path
                    in,   // query
                    null  // fragment
            );
            return uri.toASCIIString()
                    .substring(1) // remove leading ?
                    .replace("&", "%26") // encode qs separators
                    .replace("=", "%3D")
                    .replace("+", "%2B");
        } catch (URISyntaxException e) {
            throw new RuntimeException(
                    "Couldn't encode query parameter " + in,
                    e);
        }
    }
}
