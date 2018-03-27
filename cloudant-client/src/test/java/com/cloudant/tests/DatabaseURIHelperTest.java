/*
 * Copyright (c) 2015 Cloudant, Inc. All rights reserved.
 * Copyright © 2018 IBM Corp. All rights reserved.
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


package com.cloudant.tests;

import com.cloudant.client.internal.DatabaseURIHelper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

@ExtendWith(DatabaseURIHelperTest.ParameterProvider.class)
public class DatabaseURIHelperTest {

    static class ParameterProvider implements TestTemplateInvocationContextProvider {
        @Override
        public boolean supportsTestTemplate(ExtensionContext context) {
            return true;
        }

        @Override
        public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts
                (ExtensionContext context) {
            return Stream.of(invocationContext(""), invocationContext
                    ("/api/couch/account_2128459498a75498"));
        }

        public static TestTemplateInvocationContext invocationContext(final String path) {
            return new TestTemplateInvocationContext() {
                @Override
                public String getDisplayName(int invocationIndex) {
                    return String.format("path:%s", path);
                }

                @Override
                public List<Extension> getAdditionalExtensions() {
                    return Collections.<Extension>singletonList(new ParameterResolver() {
                        @Override
                        public boolean supportsParameter(ParameterContext parameterContext,
                                                         ExtensionContext extensionContext) {
                            switch (parameterContext.getIndex()) {
                                case 0:
                                    return parameterContext.getParameter().getType().equals
                                            (String.class);
                            }
                            return false;
                        }

                        @Override
                        public Object resolveParameter(ParameterContext parameterContext,
                                                       ExtensionContext extensionContext) {
                            switch (parameterContext.getIndex()) {
                                case 0:
                                    return path;
                            }
                            return null;
                        }
                    });
                }
            };
        }
    }

    String protocol = "http";
    String hostname = "127.0.0.1";
    int port = 5984;
    String uriBase;

    @BeforeEach
    public void setup(String path) {
        uriBase = protocol + "://" + hostname + ":" + port + path;
    }

    // make a helper for given db name
    private DatabaseURIHelper helper(String dbName) throws URISyntaxException {
        return new DatabaseURIHelper(new URI(protocol, null, hostname, port, dbName, null, null));
    }

    @TestTemplate
    public void _localDocumentURI(String path) throws Exception {
        final String expected = uriBase + "/db_name/_local/mylocaldoc";

        DatabaseURIHelper helper = helper(path + "/db_name");
        URI localDoc = helper.documentUri("_local/mylocaldoc");

        Assertions.assertEquals(expected, localDoc.toString());
    }

    @TestTemplate
    public void buildDbUri(String path) throws Exception {
        URI expected = new URI(uriBase + "/db_name");
        URI actual = helper(path + "/db_name").getDatabaseUri();
        Assertions.assertEquals(expected, actual);
    }

    // this test shows that non-ascii characters will be represented correctly
    // in the url but that we don't escape characters like /
    @TestTemplate
    public void buildEscapedDbUri(String path) throws Exception {
        URI expected = new URI(uriBase + "/SDF@%23%25$%23)DFGKLDfdffdg%C3%A9");
        URI actual = helper(path + "/SDF@#%$#)DFGKLDfdffdg\u00E9").getDatabaseUri();
        Assertions.assertEquals(expected.toASCIIString(), actual.toASCIIString());
    }

    @TestTemplate
    public void buildChangesUri_options_optionsEncoded(String path) throws Exception {
        URI expected = new URI(uriBase + "/test/_changes?limit=100&since=%22%5B%5D%22");

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("since", "\"[]\"");
        options.put("limit", 100);
        URI actual = helper(path + "/test").changesUri(options);
        Assertions.assertEquals(expected, actual);
    }

    @TestTemplate
    public void buildChangesUri_woOptions(String path) throws Exception {
        URI expected = new URI(uriBase + "/test/_changes");
        URI actual = helper(path + "/test").changesUri(new HashMap<String, Object>());
        Assertions.assertEquals(expected, actual);
    }

    @TestTemplate
    public void buildBulkDocsUri(String path) throws Exception {
        URI expected = new URI(uriBase + "/test/_bulk_docs");
        URI actual = helper(path + "/test").bulkDocsUri();
        Assertions.assertEquals(expected, actual);
    }

    @TestTemplate
    public void revsDiffUri(String path) throws Exception {
        URI expected = new URI(uriBase + "/test/_revs_diff");
        URI actual = helper(path + "/test").revsDiffUri();
        Assertions.assertEquals(expected, actual);
    }

    // get a document with a db 'mounted' at /
    @TestTemplate
    public void buildDocumentUri_emptyDb(String path) throws Exception {
        URI expected = new URI(uriBase + "/documentId");
        URI actual = helper(path).documentUri("documentId");
        Assertions.assertEquals(expected, actual);
    }

    @TestTemplate
    public void buildDocumentUri_woOptions(String path) throws Exception {
        URI expected = new URI(uriBase + "/test/documentId");
        URI actual = helper(path + "/test").documentUri("documentId");
        Assertions.assertEquals(expected, actual);
    }

    @TestTemplate
    public void buildDocumentUri_slashInDocumentId(String path) throws Exception {
        URI expected = new URI(uriBase + "/test/path1%2Fpath2");
        URI actual = helper(path + "/test").documentUri("path1/path2");
        Assertions.assertEquals(expected, actual);
    }

    @TestTemplate
    public void buildDocumentUri_specialCharsInDocumentId(String path) throws Exception {
        URI expected = new URI(uriBase + "/test/SDF@%23%25$%23)DFGKLDfdffdg%C3%A9");
        URI actual = helper(path + "/test").documentUri("SDF@#%$#)DFGKLDfdffdg\u00E9");
        Assertions.assertEquals(expected, actual);
    }

    @TestTemplate
    public void buildDocumentUri_colonInDocumentId(String path) throws Exception {
        URI expected = new URI(uriBase + "/test/:this:has:colons:");
        URI actual = helper(path + "/test").documentUri(":this:has:colons:");
        Assertions.assertEquals(expected, actual);
    }

    @TestTemplate
    public void buildDocumentUri_options_optionsEncoded(String path) throws Exception {
        URI expected = new URI(uriBase + "/test/path1%2Fpath2?detail=true&revs=%5B1-2%5D");

        Map<String, Object> options = new TreeMap<String, Object>();
        options.put("revs", "[1-2]");
        options.put("detail", true);
        URI actual = helper(path + "/test").documentId("path1/path2").query(options).build();
        Assertions.assertEquals(expected, actual);
    }

    @TestTemplate
    public void buildDocumentUri_options_encodeSeparators(String path) throws Exception {
        URI expected = new URI(uriBase +
                "/test/path1%2Fpath2?d%26etail%3D=%26%3D%3Dds%26&revs=%5B1-2%5D");

        TreeMap<String, Object> options = new TreeMap<String, Object>();
        options.put("revs", "[1-2]");
        options.put("d&etail=", "&==ds&");
        URI actual = helper(path + "/test").documentId("path1/path2").query(options).build();
        Assertions.assertEquals(expected, actual);
    }

    @TestTemplate
    public void buildDocumentUri_options_hasPlus(String path) throws Exception {
        URI expected = new URI(uriBase + "/test/path1%2Fpath2?q=class:mammal%2Bwith%2Bplusses");

        TreeMap<String, Object> options = new TreeMap<String, Object>();
        options.put("q", "class:mammal+with+plusses");
        URI actual = helper(path + "/test").documentId("path1/path2").query(options).build();
        Assertions.assertEquals(expected, actual);
    }


    // this test shows that non-ascii characters will be represented correctly
    // in the url but that we don't escape characters like / in the root url, but that they are
    // correctly escaped in the document part of the url
    @TestTemplate
    public void buildVeryEscapedUri(String path) throws Exception {
        URI expected = new URI(uriBase + "/SDF@%23%25$%23)KLDfdffdg%C3%A9/%2FSF@%23%25$%23)" +
                "DFGKLDfdffdg%C3%A9%2Fpath2?detail=/SDF@%23%25$%23)%C3%A9&revs=%5B1-2%5D");

        Map<String, Object> options = new TreeMap<String, Object>();
        options.put("revs", "[1-2]");
        options.put("detail", "/SDF@#%$#)\u00E9");
        URI actual = helper(path + "/SDF@#%$#)KLDfdffdg\u00E9").documentId("/SF@#%$#)"
                + "DFGKLDfdffdg\u00E9/path2").query(options).build();

        Assertions.assertEquals(expected.toASCIIString(), actual.toASCIIString());
    }


    @TestTemplate
    public void encodePathComponent_slashShouldBeEncoded(String path) throws Exception {
        String in = "/path1/path2";
        Assertions.assertEquals("%2Fpath1%2Fpath2", helper(path + "/test").encodeId(in));
    }

    @TestTemplate
    public void encodeQueryParameter_noLeadingQuestionMark(String path) throws Exception {
        String in = "a";
        Assertions.assertTrue(helper(path + "/test").documentUri(in).toString().charAt(0) != '?');
    }

    @TestTemplate
    public void buildQuery_joinTwoQueries(String path) throws Exception {
        Map<String, Object> mapOptions = new TreeMap<String, Object>();
        mapOptions.put("revs", "[1-2]");
        mapOptions.put("detail", "/SDF@#%$#)");

        String query = "boolean=true";

        URI expectedQuery = new URI(uriBase + "?detail=/SDF@%23%25$%23)" +
                "&revs=%5B1-2%5D&boolean=true");
        URI actualQuery = helper(path).query(mapOptions).query(query).build();
        Assertions.assertEquals(expectedQuery.toASCIIString(), actualQuery.toASCIIString());
    }

}
