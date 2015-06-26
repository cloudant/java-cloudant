package com.cloudant.tests;

public class Animal {

    private String _id;
    @SuppressWarnings("unused")
    private String _rev;
    private String Class;

    public void setId(String s) {
        _id = s;
    }

    public String getId() {
        return _id;
    }

    Animal(String _id) {
        super();
        this._id = _id;
        Class = "mammal";
    }


    public String getclass() {
        return Class;
    }


    public Animal setClass(String class1) {
        Class = class1;
        return this;
    }

    Animal() {
        super();

    }


}
