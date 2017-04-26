package com.cloudant.client.api.model;

/**
 * Created by tomblench on 22/03/2017.
 */
public class TextIndexField {

    private String name;

    private String type;

    public TextIndexField(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
