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

/**
 * This package provides access to the
 * <a target="_blank" href="https://docs.cloudant.com/creating_views.html">view API</a>.
 *
 * <H1>Overview</H1>
 * <P>
 * As described in the view documentation, views are a way of performing MapReduce on document
 * content in a database. This package facilitates making query requests on views defined in
 * design documents in the database.
 * </P>
 * <P>
 * Consider this example view (called "shape_sides") that has a map function emitting key-value
 * pairs of a string and an integer:
 * </P>
 * <pre>
 * function(doc) {
 *     emit(doc.shape, doc.sides);
 * }
 * </pre>
 * <P>
 * A sample document that could be queried by this view is:
 * </P>
 * <pre>
 * { "_id" : docId,
 *   "_rev" : 1-23456
 *   "shape" : "triangle"
 *   "sides" : 3
 * }
 * </pre>
 * <P>
 * The results of a query using this view are a JSON object containing rows of the document ID
 * and key-value pairs:
 * </P>
 * <pre>
 * {"total_rows":1,"offset":0,"rows":[
 * {"id":"docId","key":"triangle","value":3},
 * ]}
 * </pre>
 * <H1>Example Usage</H1>
 * <P>
 * Example usage of this API for the example view and document above:
 * </P>
 * <PRE>
 * {@code
 * //get a ViewRequestBuilder from the database for the chosen view
 * ViewRequestBuilder viewBuilder = db.getViewRequestBuilder("myDesignDoc", "shapes_sides");
 *
 * //build a new request and specify any parameters required
 * ViewRequest<String, Integer> request = viewBuilder.newRequest(Key.Type.STRING,Integer.class)
 * .startKey("square") //return docs after "square"
 * .build();
 *
 * //perform the request and get the response
 * ViewResponse<String, Integer> response = request.getResponse();
 *
 * //loop through the rows of the response
 * for (ViewResponse.Row<String, Integer> row : response.getRows()) {
 * String key = row.getKey();
 * Integer value = row.getValue();
 * System.out.println("Shape " + key + " has " + value + " sides.");
 * }
 * }
 * </PRE>
 * Would produce output like:
 * <pre>
 * Shape square has 4 sides.
 * Shape triangle has 3 sides.
 * </pre>
 * <H1>Usage Summary</H1>
 * <OL>
 * <LI>Get a {@link com.cloudant.client.api.views.ViewRequestBuilder} from the
 * {@link com.cloudant.client.api.Database}.</LI>
 * <LI>Use the ViewRequestBuilder to get a {@link com.cloudant.client.api.views.RequestBuilder}
 * for the required type of request.</LI>
 * <LI>Specify the parameters for the request using the builder's methods defined by teh type of
 * {@link com.cloudant.client.api.views.SettableViewParameters}.</LI>
 * <LI>Build the {@link com.cloudant.client.api.views.ViewRequest}.</LI>
 * <LI>From the ViewRequest optionally obtain a single result or get the complete
 * {@link com.cloudant.client.api.views.ViewResponse}.</LI>
 * <LI>Process the ViewResponse keys, values, documents or rows as needed by your application.</LI>
 * </OL>
 *
 * <H1>Migration example</H1>
 * <P>
 * This shows how to migrate some examples from the version 1.x view API
 * to the version 2.x view API.
 * </P>
 *
 * <h2>Version 1.x</h2>
 * <pre>
 * {@code
 *  List<Foo> list = db.view("example/foo")
 * 	  .startKey("start-key")
 * 	  .endKey("end-key")
 * 	  .limit(10)
 * 	  .includeDocs(true)
 * 	  .query(Foo.class);
 *
 *  // scalar values
 *  int count = db.view("example/by_tag")
 * 	  .key("couchdb")
 * 	  .queryForInt();
 *
 * // pagination
 * Page<Foo> page = db.view("example/foo").queryPage(5, null, Foo.class);
 * List<Foo> foos = page.getResultList();
 * Page<Foo> nextPage = db.view("example/foo").queryPage(5, page.getNextParam(), Foo.class);
 * }
 * </pre>
 *
 * <h2>Version 2.x</h2>
 * <pre>
 * {@code
 *  List<Foo> list = db.getViewRequestBuilder("example","foo")
 *    .newRequest(Key.Type.STRING, Object.class)
 * 	  .startKey("start-key")
 * 	  .endKey("end-key")
 * 	  .limit(10)
 * 	  .includeDocs(true)
 * 	  .build()
 * 	  .getResponse()
 * 	  .getDocsAs(Foo.class);
 *
 *  // scalar values
 *  int count = db.getViewRequestBuilder("example","by_tag")
 *    .newRequest(Key.Type.STRING, Integer.class)
 * 	  .keys("couchdb")
 * 	  .build()
 * 	  .getSingleValue();
 *
 * // pagination
 * ViewResponse<String, Object> page = db.getViewRequestBuilder("example","foo")
 *   .newPaginatedRequest(Key.Type.STRING, Object.class)
 *   .rowsPerPage(5)
 *   .includeDocs(true)
 *   .build()
 *   .getResponse();
 *
 * List<Foo> foos = page.getDocsAs(Foo.class);
 * ViewResponse<String, Object> nextPage = page.nextPage();
 * }
 * </pre>
 *
 * @since 2.0.0
 */
package com.cloudant.client.api.views;
