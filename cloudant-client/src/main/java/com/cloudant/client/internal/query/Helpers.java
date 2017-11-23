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
package com.cloudant.client.internal.query;

import java.util.LinkedList;
import java.util.List;

public class Helpers {

    public static String quote(Object o) {
        if (o.getClass().equals(String.class)) {
            // string
            return String.format("\"%s\"", o);
        } else {
            // int, float, bool
            return String.format("%s", o);
        }
    }

    public static String quote(Object[] os) {
        return quote(os, false);
    }


    public static String quote(Object[] os, boolean single) {
        if (!single && os.length == 1) {
            return quote(os[0]);
        }
        return quoteInternal(os, ", ", "", "", "[", "]");
    }

    public static String quoteNoSquare(Object[] os) {
        if (os.length == 1) {
            return quote(os[0]);
        }
        return quoteInternal(os, ", ", "", "", "", "");
    }

    public static String quoteCurly(Object[] os) {
        if (os.length == 1) {
            // the operation "not" only takes one argument, so we don't need to make an array
            return String.format("%s%s%s", "{", quote(os[0]), "}");
        }
        return quoteInternal(os, ", ", "{", "}", "[", "]");
    }

    public static String quoteCurlyNoSquare(Object[] os) {
        return quoteInternal(os, ", ", "{", "}", "", "");
    }

    private static String quoteInternal(Object[] os,
                                        String joiner,
                                        String start,
                                        String end,
                                        String arrayStart,
                                        String arrayEnd) {
        List<String> ss = new LinkedList<String>();
        for (Object o : os) {
            ss.add(quote(o));
        }
        return String.format("%s%s%s%s%s",
                arrayStart,
                start,
                joinInternal(end+joiner+start, ss.toArray(new String[0])),
                end,
                arrayEnd
        );
    }

    private static String joinInternal(String delimiter, String... args) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String arg : args) {
            sb.append(arg);
            if (++i != args.length) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

}
