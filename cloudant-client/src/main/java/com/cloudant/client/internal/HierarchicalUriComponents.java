/*
 * Copyright (c) 2015 IBM Corp. All rights reserved.
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Code based on commit a0b231d36d7baf295a91e052cd2eb02a37a16a80 of HierarchicalUriComponents.java
 * from The Spring Framework http://projects.spring.io/spring-framework version 4.1.7
 * see https://raw.githubusercontent.com/spring-projects/spring-framework/v4.1.7.RELEASE/spring-web/src/main/java/org/springframework/web/util/HierarchicalUriComponents.java
 */

package com.cloudant.client.internal;

import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.assertNotEmpty;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

public class HierarchicalUriComponents {

    /**
     * Encode the given source into an encoded String using the rules specified
     * by the given component and with the given options.
     * @param source the source string
     * @param encoding the encoding of the source string
     * @param type the URI component for the source
     * @return the encoded URI
     * @throws IllegalArgumentException when the given uri parameter is not a valid URI
     */
    public static String encodeUriComponent(String source, String encoding, Type type)
            throws UnsupportedEncodingException {

        if (source == null) {
            return null;
        }
        assertNotEmpty(encoding, "Encoding");
        byte[] bytes = encodeBytes(source.getBytes(encoding), type);
        return new String(bytes, "US-ASCII");
    }

    private static byte[] encodeBytes(byte[] source, Type type) {
        assertNotEmpty(source, "Source");
        assertNotEmpty(type, "Type");
        ByteArrayOutputStream bos = new ByteArrayOutputStream(source.length);
        for (byte b : source) {
            if (b < 0) {
                b += 256;
            }
            if (type.isAllowed(b)) {
                bos.write(b);
            }
            else {
                bos.write('%');
                char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
                char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
                bos.write(hex1);
                bos.write(hex2);
            }
        }
        return bos.toByteArray();
    }

    /**
     * Enumeration used to identify the allowed characters per URI component.
     * <p>Contains methods to indicate whether a given character is valid in a specific URI component.
     * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
     */
    public enum Type {

        SCHEME {
            @Override
            public boolean isAllowed(int c) {
                return isAlpha(c) || isDigit(c) || '+' == c || '-' == c || '.' == c;
            }
        },
        AUTHORITY {
            @Override
            public boolean isAllowed(int c) {
                return isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c;
            }
        },
        USER_INFO {
            @Override
            public boolean isAllowed(int c) {
                return isUnreserved(c) || isSubDelimiter(c) || ':' == c;
            }
        },
        HOST_IPV4 {
            @Override
            public boolean isAllowed(int c) {
                return isUnreserved(c) || isSubDelimiter(c);
            }
        },
        HOST_IPV6 {
            @Override
            public boolean isAllowed(int c) {
                return isUnreserved(c) || isSubDelimiter(c) || '[' == c || ']' == c || ':' == c;
            }
        },
        PORT {
            @Override
            public boolean isAllowed(int c) {
                return isDigit(c);
            }
        },
        PATH {
            @Override
            public boolean isAllowed(int c) {
                return isPchar(c) || '/' == c;
            }
        },
        PATH_SEGMENT {
            @Override
            public boolean isAllowed(int c) {
                return isPchar(c);
            }
        },
        QUERY {
            @Override
            public boolean isAllowed(int c) {
                return isPchar(c) || '/' == c || '?' == c;
            }
        },
        QUERY_PARAM {
            @Override
            public boolean isAllowed(int c) {
                // The query component defined in RFC 3986 does not specify the structure of data
                // within a query or query parameter, which is deferred to the URI's scheme.
                // The HTTP scheme in turn does not specify any restrictions on the structure of
                // data within the query component, only that it must conform to RFC 3986.
                // As a result the valid characters are determined by the server processing the
                // request, so in this case we defer to the HTML application/x-www-form-urlencoded
                // specification https://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.1
                // and appendix B.2.2 https://www.w3.org/TR/html401/appendix/notes.html#h-B.2.2
                // to force encoding of the following:
                //   '+' as otherwise it would interpreted as a space
                //   '=' as the key value separator
                //   '&' and ';' as query parameter delimiters
                // before deferring to RFC 3986.
                if ('=' == c || '+' == c || '&' == c || ';' == c) {
                    return false;
                } else {
                    // These are the RFC 3986 allowed characters for a query
                    return isPchar(c) || '/' == c || '?' == c;
                }
            }
        },
        FRAGMENT {
            @Override
            public boolean isAllowed(int c) {
                return isPchar(c) || '/' == c || '?' == c;
            }
        },
        URI {
            @Override
            public boolean isAllowed(int c) {
                return isUnreserved(c);
            }
        };

        /**
         * Indicates whether the given character is allowed in this URI component.
         * @return {@code true} if the character is allowed; {@code false} otherwise
         */
        public abstract boolean isAllowed(int c);

        /**
         * Indicates whether the given character is in the {@code ALPHA} set.
         * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
         */
        protected boolean isAlpha(int c) {
            return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
        }

        /**
         * Indicates whether the given character is in the {@code DIGIT} set.
         * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
         */
        protected boolean isDigit(int c) {
            return c >= '0' && c <= '9';
        }

        /**
         * Indicates whether the given character is in the {@code gen-delims} set.
         * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
         */
        protected boolean isGenericDelimiter(int c) {
            return ':' == c || '/' == c || '?' == c || '#' == c || '[' == c || ']' == c || '@' == c;
        }

        /**
         * Indicates whether the given character is in the {@code sub-delims} set.
         * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
         */
        protected boolean isSubDelimiter(int c) {
            return '!' == c || '$' == c || '&' == c || '\'' == c || '(' == c || ')' == c || '*' == c || '+' == c ||
                    ',' == c || ';' == c || '=' == c;
        }

        /**
         * Indicates whether the given character is in the {@code reserved} set.
         * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
         */
        protected boolean isReserved(int c) {
            return isGenericDelimiter(c) || isSubDelimiter(c);
        }

        /**
         * Indicates whether the given character is in the {@code unreserved} set.
         * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
         */
        protected boolean isUnreserved(int c) {
            return isAlpha(c) || isDigit(c) || '-' == c || '.' == c || '_' == c || '~' == c;
        }

        /**
         * Indicates whether the given character is in the {@code pchar} set.
         * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
         */
        protected boolean isPchar(int c) {
            return isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c;
        }
    }

}
