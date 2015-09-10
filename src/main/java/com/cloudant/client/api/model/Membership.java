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
