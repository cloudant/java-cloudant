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

package com.cloudant.client.api.model;

import com.cloudant.client.api.DbDesign;


/**
 * Represents a design document.
 *
 * @author Ganesh K Choudhary
 * @see DbDesign
 * @since 0.0.1
 */
public class DesignDocument extends com.cloudant.client.org.lightcouch.DesignDocument {


    public class MapReduce {
        private com.cloudant.client.org.lightcouch.DesignDocument.MapReduce mapReduce;

        public String getMap() {
            return mapReduce.getMap();
        }

        public String getReduce() {
            return mapReduce.getReduce();
        }

        public void setMap(String map) {
            mapReduce.setMap(map);
        }

        public void setReduce(String reduce) {
            mapReduce.setReduce(reduce);
        }

        public int hashCode() {
            return mapReduce.hashCode();
        }

        public boolean equals(Object obj) {
            return mapReduce.equals(obj);
        }

        com.cloudant.client.org.lightcouch.DesignDocument.MapReduce getMapReduce() {
            return mapReduce;
        }

    }
}
