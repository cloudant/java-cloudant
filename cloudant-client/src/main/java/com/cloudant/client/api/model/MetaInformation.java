/*
 * Copyright © 2018 IBM Corp. All rights reserved.
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

package com.cloudant.client.api.model;

import java.util.List;

public class MetaInformation {

    private String couchdb;
    private String uuid;
    private String version;
    private Vendor vendor;
    private List<String> features;

    public MetaInformation(String couchdb, String uuid, String version, Vendor vendor,
                           List<String> features) {
        this.couchdb = couchdb;
        this.uuid = uuid;
        this.version = version;
        this.vendor = vendor;
        this.features = features;
    }

    public String getCouchdb() {
        return couchdb;
    }

    // optional
    public String getUuid() {
        return uuid;
    }

    public String getVersion() {
        return version;
    }

    public Vendor getVendor() {
        return vendor;
    }

    // optional
    public List<String> getFeatures() {
        return features;
    }

    public static class Vendor {

        private String name;
        private String version;
        private String variant;

        public Vendor(String name, String version, String variant) {
            this.name = name;
            this.version = version;
            this.variant = variant;
        }

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
                ", uuid='" + uuid + '\'' +
                ", version='" + version + '\'' +
                ", vendor=" + vendor +
                ", features=" + features +
                '}';
    }
}

