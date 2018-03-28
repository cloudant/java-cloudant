/*
 * Copyright © 2017, 2018 IBM Corp. All rights reserved.
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

import static com.cloudant.client.api.query.Expression.all;
import static com.cloudant.client.api.query.Expression.eq;
import static com.cloudant.client.api.query.Expression.exists;
import static com.cloudant.client.api.query.Expression.gt;
import static com.cloudant.client.api.query.Expression.gte;
import static com.cloudant.client.api.query.Expression.in;
import static com.cloudant.client.api.query.Expression.lt;
import static com.cloudant.client.api.query.Expression.lte;
import static com.cloudant.client.api.query.Expression.nin;
import static com.cloudant.client.api.query.Expression.type;
import static com.cloudant.client.api.query.Operation.and;
import static com.cloudant.client.api.query.Operation.nor;
import static com.cloudant.client.api.query.Operation.not;
import static com.cloudant.client.api.query.Operation.or;
import static com.cloudant.client.api.query.PredicatedOperation.elemMatch;

import com.cloudant.client.api.query.PredicateExpression;
import com.cloudant.client.api.query.QueryBuilder;
import com.cloudant.client.api.query.Sort;
import com.cloudant.client.api.query.Type;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class QueryTests {

    // "Selector basics"
    @Test
    public void basicSelector1() {
        QueryBuilder qb = new QueryBuilder(eq("director", "Lars von Trier"));
        Assertions.assertEquals("{\"selector\": {\"director\": {\"$eq\": \"Lars von Trier\"}}}",
                qb.build());
    }

    // "Selector with two fields"
    @Test
    public void basicSelector2() {
        QueryBuilder qb = new QueryBuilder(and(
                eq("name", "Paul"),
                eq("location", "Boston")));
        Assertions.assertEquals("{\"selector\": {\"$and\": [{\"name\": {\"$eq\": \"Paul\"}}, " +
                "{\"location\": {\"$eq\": \"Boston\"}}]}}", qb.build());
    }

    // "SUBFIELDS"
    @Test
    public void basicSelector3() {
        QueryBuilder qb = new QueryBuilder(eq("imdb.rating", 8));
        Assertions.assertEquals("{\"selector\": {\"imdb.rating\": {\"$eq\": 8}}}", qb.build());
    }

    // "Example selector using an operator to match any document, where the age field has a value
    // greater than 20:"
    @Test
    public void basicSelector4() {
        QueryBuilder qb = new QueryBuilder(gt("year", 2018));
        Assertions.assertEquals("{\"selector\": {\"year\": {\"$gt\": 2018}}}", qb.build());
    }

    // "$and operator used with full text indexing"
    @Test
    public void basicSelector5() {
        QueryBuilder qb = new QueryBuilder(and(
                eq("$text", "Schwarzenegger"),
                in("year", 1984, 1991)
        ));
        Assertions.assertEquals("{\"selector\": {\"$and\": [{\"$text\": {\"$eq\": " +
                "\"Schwarzenegger\"}}, {\"year\": {\"$in\": [1984, 1991]}}]}}", qb.build());
    }

    // "$and operator used with full text indexing"
    @Test
    public void basicSelector5_single() {
        QueryBuilder qb = new QueryBuilder(and(
                eq("$text", "Schwarzenegger"),
                in("year", 1984)
        ));
        Assertions.assertEquals("{\"selector\": {\"$and\": [{\"$text\": {\"$eq\": " +
                "\"Schwarzenegger\"}}, {\"year\": {\"$in\": [1984]}}]}}", qb.build());
    }

    // "$or operator used with full text indexing"
    @Test
    public void basicSelector6() {
        QueryBuilder qb = new QueryBuilder(or(
                eq("director", "George Lucas"),
                eq("director", "Steven Spielberg")
        ));
        Assertions.assertEquals("{\"selector\": {\"$or\": [{\"director\": {\"$eq\": \"George " +
                "Lucas\"}}, {\"director\": {\"$eq\": \"Steven Spielberg\"}}]}}", qb.build());
    }

    // "$or operator used with database indexed on the field "year"
    @Test
    public void basicSelector7() {
        QueryBuilder qb = new QueryBuilder(and(
                eq("year", 1977),
                or(
                        eq("director", "George Lucas"),
                        eq("director", "Steven Spielberg")
                )
        ));
        Assertions.assertEquals("{\"selector\": {\"$and\": [{\"year\": {\"$eq\": 1977}}, " +
                "{\"$or\": [{\"director\": {\"$eq\": \"George Lucas\"}}, {\"director\": {\"$eq\":" +
                " \"Steven Spielberg\"}}]}]}}", qb.build());
    }

    // "$not operator used with database indexed on the field "year""
    @Test
    public void basicSelector8() {
        QueryBuilder qb = new QueryBuilder(and(
                gte("year", 1900),
                lte("year", 1903),
                not(eq("year", 1901))
        ));
        Assertions.assertEquals("{\"selector\": {\"$and\": [{\"year\": {\"$gte\": 1900}}, " +
                "{\"year\": {\"$lte\": 1903}}, {\"$not\": {\"year\": {\"$eq\": 1901}}}]}}", qb
                .build());
    }

    // "$nor operator used with database indexed on the field "year""
    @Test
    public void basicSelector9() {
        QueryBuilder qb = new QueryBuilder(and(
                gte("year", 1900),
                lte("year", 1910),
                nor(
                        eq("year", 1901),
                        eq("year", 1905),
                        eq("year", 1907))
        ));
        Assertions.assertEquals("{\"selector\": {\"$and\": [{\"year\": {\"$gte\": 1900}}, " +
                "{\"year\": {\"$lte\": 1910}}, {\"$nor\": [{\"year\": {\"$eq\": 1901}}, " +
                "{\"year\": {\"$eq\": 1905}}, {\"year\": {\"$eq\": 1907}}]}]}}", qb.build());
    }

    // "$all operator used with full text indexing"
    @Test
    public void basicSelector10() {
        QueryBuilder qb = new QueryBuilder(all("genre", "Comedy", "Short"));
        Assertions.assertEquals("{\"selector\": {\"genre\": {\"$all\": [\"Comedy\", " +
                "\"Short\"]}}}", qb.build());
    }

    // "$all operator used with full text indexing"
    @Test
    public void basicSelector10_single() {
        QueryBuilder qb = new QueryBuilder(all("genre", "Comedy"));
        Assertions.assertEquals("{\"selector\": {\"genre\": {\"$all\": [\"Comedy\"]}}}", qb.build
                ());
    }

    // "elemMatch operator used with full text indexing"
    @Test
    public void basicSelector11() {
        QueryBuilder qb = new QueryBuilder(elemMatch("genre", PredicateExpression.eq("Horror")));
        Assertions.assertEquals("{\"selector\": {\"genre\": {\"$elemMatch\": {\"$eq\": " +
                "\"Horror\"}}}}", qb.build());
    }

    // "$lt operator used with database indexed on the field "year""
    // (similar for all (in)equality tests, so one is representative
    @Test
    public void basicSelector12() {
        QueryBuilder qb = new QueryBuilder(lt("year", 1900));
        Assertions.assertEquals("{\"selector\": {\"year\": {\"$lt\": 1900}}}", qb.build());
    }

    // "$exists operator used with database indexed on the field "year""
    @Test
    public void basicSelector13() {
        QueryBuilder qb = new QueryBuilder(and(eq("year", 2015), exists("title", true)));
        Assertions.assertEquals("{\"selector\": {\"$and\": [{\"year\": {\"$eq\": 2015}}, " +
                "{\"title\": {\"$exists\": true}}]}}", qb.build());
    }

    // "$type operator used with full text indexing"
    @Test
    public void basicSelector14() {
        QueryBuilder qb = new QueryBuilder(type("year", Type.NUMBER));
        Assertions.assertEquals("{\"selector\": {\"year\": {\"$type\": \"number\"}}}", qb.build());
    }

    // "$in operator used with full text indexing"
    @Test
    public void basicSelector15() {
        QueryBuilder qb = new QueryBuilder(in("year", 2010, 2015));
        Assertions.assertEquals("{\"selector\": {\"year\": {\"$in\": [2010, 2015]}}}", qb.build());
    }

    // "$in operator used with full text indexing"
    @Test
    public void basicSelector15_single() {
        QueryBuilder qb = new QueryBuilder(in("year", 2010));
        Assertions.assertEquals("{\"selector\": {\"year\": {\"$in\": [2010]}}}", qb.build());
    }

    // "$nin operator used with full text indexing"
    @Test
    public void basicSelector16() {
        QueryBuilder qb = new QueryBuilder(and(gt("year", 2009),
                nin("year", 2010, 2015)));
        Assertions.assertEquals("{\"selector\": {\"$and\": [{\"year\": {\"$gt\": 2009}}, " +
                "{\"year\": {\"$nin\": [2010, 2015]}}]}}", qb.build());
    }

    // "$nin operator used with full text indexing"
    @Test
    public void basicSelector16_single() {
        QueryBuilder qb = new QueryBuilder(and(gt("year", 2009),
                nin("year", 2010)));
        Assertions.assertEquals("{\"selector\": {\"$and\": [{\"year\": {\"$gt\": 2009}}, " +
                "{\"year\": {\"$nin\": [2010]}}]}}", qb.build());
    }

    @Test
    public void complexSelector1() {
        QueryBuilder qb = new QueryBuilder(not(and(gt("year", 2009),
                nin("year", 2010, 2015))));
        Assertions.assertEquals("{\"selector\": {\"$not\": {\"$and\": [{\"year\": {\"$gt\": " +
                "2009}}, {\"year\": {\"$nin\": [2010, 2015]}}]}}}", qb.build());
    }

    @Test
    public void complexSelector2() {
        QueryBuilder qb = new QueryBuilder(or(
                and(
                        eq("Actor", "Schwarzenegger"),
                        eq("Year", 2012)),
                and(
                        eq("Actor", "de Vito"),
                        eq("Year", 2001))
        ));
        Assertions.assertEquals("{\"selector\": {\"$or\": [{\"$and\": [{\"Actor\": {\"$eq\": " +
                "\"Schwarzenegger\"}}, {\"Year\": {\"$eq\": 2012}}]}, {\"$and\": [{\"Actor\": " +
                "{\"$eq\": \"de Vito\"}}, {\"Year\": {\"$eq\": 2001}}]}]}}", qb.build());
    }

    // "Selector basics"
    @Test
    public void basicSelector1WithFields() {
        QueryBuilder qb = new QueryBuilder(eq("director", "Lars von Trier")).fields("_id",
                "_rev", "year", "title");
        Assertions.assertEquals("{\"selector\": {\"director\": {\"$eq\": \"Lars von Trier\"}}, " +
                "\"fields\": [\"_id\", \"_rev\", \"year\", \"title\"]}", qb.build());
    }

    // "Selector basics"
    @Test
    public void basicSelector1WithSort() {
        QueryBuilder qb = new QueryBuilder(eq("director", "Lars von Trier")).sort(Sort.asc
                ("year"), Sort.desc("director"));
        Assertions.assertEquals("{\"selector\": {\"director\": {\"$eq\": \"Lars von Trier\"}}, " +
                "\"sort\": [{\"year\": \"asc\"}, {\"director\": \"desc\"}]}", qb.build());
    }

    // "Selector basics"
    @Test
    public void basicSelector1WithAllOptions() {
        QueryBuilder qb = new QueryBuilder(eq("director", "Lars von Trier")).
                fields("_id", "_rev", "year", "title").
                sort(Sort.asc("year"), Sort.desc("director")).
                limit(10).
                skip(0);
        Assertions.assertEquals("{\"selector\": {\"director\": {\"$eq\": \"Lars von Trier\"}}, " +
                "\"fields\": [\"_id\", \"_rev\", \"year\", \"title\"], " +
                "\"sort\": [{\"year\": \"asc\"}, {\"director\": \"desc\"}], \"limit\": 10, " +
                "\"skip\": 0}", qb.build());
    }


}
