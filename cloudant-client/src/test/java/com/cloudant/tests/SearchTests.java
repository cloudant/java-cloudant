/*
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

import static com.cloudant.tests.CloudantClientHelper.getReplicationSourceUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudant.client.api.DesignDocumentManager;
import com.cloudant.client.api.Search;
import com.cloudant.client.api.model.DesignDocument;
import com.cloudant.client.api.model.SearchResult;
import com.cloudant.client.api.model.SearchResult.SearchResultRow;
import com.cloudant.client.internal.URIBase;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.tests.base.TestWithDbPerClass;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@RequiresCloudant
public class SearchTests extends TestWithDbPerClass {

    @BeforeAll
    public static void setUp() throws Exception {
        // replicate the animals db for search tests
        com.cloudant.client.api.Replication r = account.replication();
        r.source(getReplicationSourceUrl("animaldb"));
        r.createTarget(true);
        r.target(dbResource.getDbURIWithUserInfo());
        r.trigger();

        // sync the design doc for faceted search
        File designDocViews101 =
                new File(String.format("%s/views101_design_doc.js", new File(System.getProperty(
                        "user.dir") + "/src/test/resources/design-files")));
        DesignDocument designDoc = DesignDocumentManager.fromFile(designDocViews101);
        db.getDesignDocumentManager().put(designDoc);
    }

    @Test
    public void searchCountsTest() {

        // do a faceted search for counts
        Search srch = db.search("views101/animals");
        SearchResult<Animal> rslt = srch.limit(10)
                .includeDocs(true)
                .counts(new String[]{"class", "diet"})
                .querySearchResult("l*", Animal.class);
        assertNotNull(rslt);
        assertNotNull(rslt.getCounts());
        assertEquals(2, rslt.getCounts().keySet().size());
        assertEquals(1, rslt.getCounts().get("class").keySet().size());
        assertEquals(new Long(2), rslt.getCounts().get("class").get("mammal"));
        assertEquals(2, rslt.getCounts().get("diet").keySet().size());
        assertEquals(new Long(1), rslt.getCounts().get("diet").get("herbivore"));
        assertEquals(new Long(1), rslt.getCounts().get("diet").get("omnivore"));
        assertNotNull(rslt.getBookmark());
        assertEquals(0, rslt.getGroups().size());
        assertEquals(2, rslt.getRows().size());
        for (@SuppressWarnings("rawtypes") SearchResultRow r : rslt.getRows()) {
            assertNotNull(r.getDoc());
            assertNotNull(r.getFields());
            assertNotNull(r.getId());
            assertNotNull(r.getOrder());
        }

    }

    @Test
    public void rowsTest() {
        Search srch = db.search("views101/animals");
        List<Animal> animals = srch.limit(10)
                .includeDocs(true)
                .counts(new String[]{"class", "diet"})
                .query("l*", Animal.class);
        assertNotNull(animals);
        assertEquals(2, animals.size());
        for (Animal a : animals) {
            assertNotNull(a);
        }
    }

    @Test
    public void groupsTest() {
        Search srch = db.search("views101/animals");
        Map<String, List<Animal>> groups = srch.limit(10)
                .includeDocs(true)
                .counts(new String[]{"class", "diet"})
                .groupField("class", false)
                .queryGroups("l*", Animal.class);
        assertNotNull(groups);
        assertEquals(1, groups.size());
        for (Entry<String, List<Animal>> g : groups.entrySet()) {
            assertNotNull(g.getKey());
            assertEquals(2, g.getValue().size());
            for (Animal a : g.getValue()) {
                assertNotNull(a);
            }
        }
    }

    @Test
    public void rangesTest() {
        // do a faceted search for ranges
        Search srch = db.search("views101/animals");
        SearchResult<Animal> rslt = srch.includeDocs(true)
                .counts(new String[]{"class", "diet"})
                .ranges("{ \"min_length\": {\"small\": \"[0 TO 1.0]\","
                        + "\"medium\": \"[1.1 TO 3.0]\", \"large\": \"[3.1 TO 9999999]\"} }")
                .querySearchResult("class:mammal", Animal.class);
        assertNotNull(rslt);
        assertNotNull(rslt.getRanges());
        assertEquals(1, rslt.getRanges().entrySet().size());
        assertEquals(3, rslt.getRanges().get("min_length").entrySet().size());
        assertEquals(new Long(3), rslt.getRanges().get("min_length").get("small"));
        assertEquals(new Long(3), rslt.getRanges().get("min_length").get("medium"));
        assertEquals(new Long(2), rslt.getRanges().get("min_length").get("large"));

    }

    @Test
    public void sortTest() {
        // do a faceted search for counts
        Search srch = db.search("views101/animals");
        SearchResult<Animal> rslt = srch.includeDocs(true)
                .sort("[\"diet<string>\"]")
                .querySearchResult("class:mammal", Animal.class);
        assertNotNull(rslt);
        assertEquals("carnivore", rslt.getRows().get(0).getOrder()[0].toString());
        assertEquals("herbivore", rslt.getRows().get(1).getOrder()[0].toString());
        assertEquals("omnivore", rslt.getRows().get(5).getOrder()[0].toString());
    }

    @Test
    public void groupSortTest() {
        Search srch = db.search("views101/animals");
        Map<String, List<Animal>> groups = srch.includeDocs(true)
                .groupField("diet", false)
                .groupSort("[\"-diet<string>\"]")
                .queryGroups("l*", Animal.class);
        assertNotNull(groups);
        assertEquals(2, groups.size());
        Iterator<String> it = groups.keySet().iterator();
        assertEquals("omnivore", it.next()); // diet in reverse order
        assertEquals("herbivore", it.next());
    }

    @Test
    public void drillDownTest() {
        // do a faceted search for drilldown
        Search srch = db.search("views101/animals");
        SearchResult<Animal> rslt = srch.includeDocs(true)
                .counts(new String[]{"class", "diet"})
                .ranges("{ \"min_length\": {\"small\": \"[0 TO 1.0]\","
                        + "\"medium\": \"[1.1 TO 3.0]\", \"large\": \"[3.1 TO 9999999]\"} }")
                .drillDown("class", "mammals")
                .querySearchResult("class:mammal", Animal.class);
        assertNotNull(rslt);
        assertNotNull(rslt.getRanges());
        assertEquals(1, rslt.getRanges().entrySet().size());
        assertEquals(3, rslt.getRanges().get("min_length").entrySet().size());
        assertEquals(new Long(0), rslt.getRanges().get("min_length").get("small"));
        assertEquals(new Long(0), rslt.getRanges().get("min_length").get("medium"));
        assertEquals(new Long(0), rslt.getRanges().get("min_length").get("large"));
    }

    @Test
    public void multiValueDrillDownTest() {
        // do a faceted search for multi-value drilldown
        Search srch = db.search("views101/animals");
        SearchResult<Animal> rslt = srch.includeDocs(true)
                .drillDown("diet", "carnivore", "omnivore")
                .querySearchResult("class:mammal", Animal.class);
        assertNotNull(rslt);
        assertEquals(4, rslt.getTotalRows());

        List<String> ids = new ArrayList<String>();
        for (SearchResultRow row : rslt.getRows()) {
            ids.add(row.getId());
        }
        List<String> expectedIds = Arrays.asList("panda", "aardvark", "badger", "lemur");
        assertTrue(ids.containsAll(expectedIds));
    }

    @Test
    /**
     * Request a search with two drilldown queries against the views101
     * design document. The design document only contains one animal
     * that satisfies the drilldown requirements.
     * Assert that the result is one animal in the class bird, and
     * that the only existing min_length value of this animal is small.
     */
    public void multipleDrillDownTest() {
        Search srch = db.search("views101/animals");
        SearchResult<Animal> rslt = srch.includeDocs(true)
                .counts(new String[]{"class", "diet"})
                .ranges("{ \"min_length\": {\"small\": \"[0 TO 1.0]\","
                        + "\"medium\": \"[1.1 TO 3.0]\", \"large\": \"[3.1 TO 9999999]\"} }")
                .drillDown("class", "bird")
                .drillDown("diet", "omnivore")
                .querySearchResult("class:bird", Animal.class);
        assertNotNull(rslt);
        assertNotNull(rslt.getRanges());
        assertEquals(1, rslt.getRanges().entrySet().size());
        assertEquals(3, rslt.getRanges().get("min_length").entrySet().size());
        assertEquals(new Long(1), rslt.getRanges().get("min_length").get("small"));
        assertEquals(new Long(0), rslt.getRanges().get("min_length").get("medium"));
        assertEquals(new Long(0), rslt.getRanges().get("min_length").get("large"));
    }

    @Test
    public void bookmarkTest() {
        Search srch = db.search("views101/animals");
        SearchResult<Animal> rslt = srch.limit(4)
                .querySearchResult("class:mammal", Animal.class);

        Search srch1 = db.search("views101/animals");
        srch1.bookmark(rslt.getBookmark())
                .querySearchResult("class:mammal", Animal.class);

    }

    private void escapingTest(String expectedResult, String query) {
        URI uri = new URIBase(account.getBaseUri())
                .path("animaldb").path("_design").path("views101").path("_search").path("animals")
                .query("include_docs", true)
                .query("q", query).build();

        String uriBaseString = account.getBaseUri().toASCIIString();

        String expectedUriString = uriBaseString
                + "/animaldb/_design/views101/_search/animals?include_docs=true&q=" +
                expectedResult;

        String uriString = uri.toASCIIString();
        assertEquals(expectedUriString, uriString);
    }

    @Test
    public void escapedPlusTest() {
        escapingTest("class:mammal%2Btest%2Bescaping", "class:mammal+test+escaping");
    }

    @Test
    public void escapedEqualsTest() {
        escapingTest("class:mammal%3Dtest%3Descaping", "class:mammal=test=escaping");
    }

    @Test
    public void escapedAmpersandTest() {
        escapingTest("class:mammal%26test%26escaping", "class:mammal&test&escaping");
    }

}

