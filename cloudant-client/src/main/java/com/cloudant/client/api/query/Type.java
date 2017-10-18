package com.cloudant.client.api.query;

import java.util.Locale;

public enum Type {

    BOOLEAN,
    STRING,
    NUMBER;

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ENGLISH);
    }
}
