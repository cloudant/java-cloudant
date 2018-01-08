/*
 * Copyright © 2017 IBM Corp. All rights reserved.
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

package com.cloudant.client.api.query;

import java.util.List;

public interface Index<F extends Field> {

    /**
     * @return the design document ID
     */
    String getDesignDocumentID();

    /**
     * @return the name of the index
     */
    String getName();

    /**
     * @return the type of the index
     */
    String getType();

    /**
     * Get the JSON string representation of the selector configured for this index.
     *
     * @return selector JSON as string
     */
    String getPartialFilterSelector();
    /**
     * @return the list of fields in the index
     */
    List<F> getFields();
}
