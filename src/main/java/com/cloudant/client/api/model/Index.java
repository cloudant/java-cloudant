package com.cloudant.client.api.model;

import com.cloudant.client.api.model.IndexField.SortOrder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates a Cloudant Index definition
 *
 * @author Mario Briggs
 * @since 0.0.1
 */
public class Index {

    private String ddoc;
    private String name;
    private String type;
    private List<IndexField> def = new ArrayList<IndexField>();

    /**
     * @return the designDocId
     */
    public String getDesignDocId() {
        return ddoc;
    }


    /**
     * @return the index name
     */
    public String getName() {
        return name;
    }


    /**
     * @return the index type e.g. json
     */
    public String getType() {
        return type;
    }


    /**
     * @return the IndexFields
     */
    public Iterator<IndexField> getFields() {
        return def.iterator();
    }

    public String toString() {
        String index = "ddoc: " + ddoc + ", name: " + name + ", type: " + type + ", fields: [";
        Iterator<IndexField> flds = getFields();
        while (flds.hasNext()) {
            index += flds.next().toString() + ",";
        }
        index += "]";
        return index;
    }


    public Index(String designDocId, String name, String type) {
        this.ddoc = designDocId;
        this.name = name;
        this.type = type;
    }

    public void addIndexField(String fieldName, SortOrder order) {
        def.add(new IndexField(fieldName, order));
    }
}


