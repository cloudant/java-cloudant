package com.cloudant.client.org.lightcouch;

import java.util.List;

public class MetaInformation {

    private String couchdb;
    private String version;
    private Vendor vendor;
    private List<String> features;

    public String getCouchdb() {
        return couchdb;
    }

    public String getVersion() {
        return version;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public List<String> getFeatures() {
        return features;
    }

    public class Vendor {
        private String name;
        private String version;
        private String variant;

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getVariant() {
            return variant;
        }

        @Override
        public String toString() {
            return "Vendor{" +
                    "name='" + name + '\'' +
                    ", version='" + version + '\'' +
                    ", variant='" + variant + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "MetaInformation{" +
                "couchdb='" + couchdb + '\'' +
                ", version='" + version + '\'' +
                ", vendor=" + vendor +
                ", features=" + features +
                '}';
    }
}

