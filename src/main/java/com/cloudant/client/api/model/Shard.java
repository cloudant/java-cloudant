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
 * Encapsulates info about a Cloudant Shard
 *
 * @author Mario Briggs
 * @since 0.0.1
 */
public class Shard {

    private String range;
    private List<String> nodes;

    /**
     * @return the range
     */
    public String getRange() {
        return range;
    }

    /**
     * @return the nodeNames in this shard
     */
    public Iterator<String> getNodes() {
        return nodes.iterator();
    }

    public Shard(String range, List<String> nodes) {
        super();
        this.range = range;
        this.nodes = nodes;
    }
}


