package com.cloudant.client.api.model;

import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates the list of nodes in a Cloudant cluster
 *
 * @author Mario Briggs
 * @since 0.0.1
 */
public class Membership {

    private List<String> all_nodes;
    private List<String> cluster_nodes;

    /**
     * @return the all_nodes
     */
    public Iterator<String> getAllNodes() {
        return all_nodes.iterator();
    }

    /**
     * @return the cluster_nodes
     */
    public Iterator<String> getClusterNodes() {
        return cluster_nodes.iterator();
    }

    public String toString() {
        String ret = "all_nodes: ";
        for (String s : all_nodes) {
            ret += s + ",";
        }
        ret += " cluster_nodes: ";
        for (String s : cluster_nodes) {
            ret += s + ",";
        }
        return ret;
    }

    Membership() {
        super();
    }
}
