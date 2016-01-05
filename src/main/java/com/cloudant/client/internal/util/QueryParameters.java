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

package com.cloudant.client.internal.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * This class can be extended by any class using {@link QueryParameter} annotations on public
 * fields.
 * It provides methods to process those parameters for adding as URL query parameters.
 */
public class QueryParameters {

    protected Map<String, Object> processParameters() {
        Map<String, Object> parameters = new HashMap<String, Object>();
        for (Field field : this.getClass().getFields()) {
            QueryParameter parameter = field.getAnnotation(QueryParameter.class);
            if (parameter != null) {
                //use the field name as the parameter name unless one was specified
                String parameterName = QueryParameter.USE_FIELD_NAME.equals(parameter.value()) ?
                        field.getName() : parameter.value();
                Object parameterValue = null;
                try {
                    parameterValue = field.get(this);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("A field annotated with @QueryParameter did not " +
                            "have the public modifier and as such was not accessible", e);
                }
                if (parameterName != null && parameterValue != null) {
                    parameters.put(parameterName, parameterValue);
                }
            }
        }
        return parameters;
    }
}
