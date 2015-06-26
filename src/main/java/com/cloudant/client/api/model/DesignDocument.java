package com.cloudant.client.api.model;

import com.cloudant.client.api.DbDesign;


/**
 * Represents a design document.
 *
 * @author Ganesh K Choudhary
 * @see DbDesign
 * @since 0.0.1
 */
public class DesignDocument extends org.lightcouch.DesignDocument {


    public class MapReduce {
        private org.lightcouch.DesignDocument.MapReduce mapReduce;

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

        org.lightcouch.DesignDocument.MapReduce getMapReduce() {
            return mapReduce;
        }

    }
}
