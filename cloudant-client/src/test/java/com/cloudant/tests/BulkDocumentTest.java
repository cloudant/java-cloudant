/*
 * Copyright (C) 2011 lightcouch.org
 * Copyright © 2015, 2018 IBM Corp. All rights reserved.
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isOneOf;

import com.cloudant.client.api.model.DesignDocument;
import com.cloudant.client.api.model.Response;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.base.TestWithDbPerClass;
import com.cloudant.tests.base.TestWithDbPerTest;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

@RequiresDB
public class BulkDocumentTest extends TestWithDbPerTest {

    @Test
    public void bulkModifyDocs() {
        List<Object> newDocs = new ArrayList<Object>();
        newDocs.add(new Foo());
        newDocs.add(new JsonObject());

        List<Response> responses = db.bulk(newDocs);

        assertThat(responses.size(), is(2));
    }

    @Test
    public void bulkRejectedByValidation() {
        // create validation fn
        DesignDocument ddoc = new DesignDocument();
        ddoc.setId("_design/validationDesignDoc");
        ddoc.setValidateDocUpdate("function (newDoc, oldDoc, userCtx, secObj) {\n" +
                "    if (newDoc['title'] == 'forbidden') {\n" +
                "        throw({forbidden: 'field cannot be forbidden' });\n" +
                "    }\n" +
                "}\n");
        db.save(ddoc);
        // create docs, one which will fail validation
        List<Object> newDocs = new ArrayList<Object>();
        newDocs.add(new Foo("doc1", "This is the document title"));
        newDocs.add(new Foo("doc2", "forbidden"));
        List<Response> responses = db.bulk(newDocs);
        assertThat(responses, hasSize(2));
        // Note that the CouchDB documents seem to be contradictory here, 201 always appears to be
        // returned even if there are documents which fail validation.
        // The documentation initially states
        // "417 Expectation Failed – Occurs when at least one document was rejected by a validation
        // function", but then later states
        // "The return type from a bulk insertion will be 201 Created, with the content of the
        // returned structure indicating specific success or otherwise messages on a per-document basis."
        // Also note that the IBM Cloudant documentation makes no mention of a 417 status code in
        // the documentation for _bulk_docs.
        // We check for 201 or 202: "Note: If the write quorum cannot be met during an attempt to
        // create a document, a 202 response is returned."
        assertThat(responses.get(0).getStatusCode(), isOneOf(201, 202));
        assertThat(responses.get(0).getReason(), is(nullValue()));
        assertThat(responses.get(0).getError(), is(nullValue()));
        assertThat(responses.get(1).getStatusCode(), isOneOf(201, 202));
        assertThat(responses.get(1).getReason(), is("field cannot be forbidden"));
        assertThat(responses.get(1).getError(), is("forbidden"));
    }

    @Test
    public void bulkDocsRetrieve() throws Exception {
        Response r1 = db.save(new Foo());
        Response r2 = db.save(new Foo());

        List<Foo> docs = db.getAllDocsRequestBuilder().includeDocs(true).keys(r1.getId(), r2
                .getId()).build().getResponse().getDocsAs(Foo.class);

        assertThat(docs.size(), is(2));
    }

}
