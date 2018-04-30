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

package com.cloudant.client.internal.views;

import com.cloudant.client.api.model.Document;
import com.cloudant.client.api.views.ViewResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

class ViewResponseImpl<K, V> implements ViewResponse<K, V> {

    protected final ViewQueryParameters<K, V> initialQueryParameters;
    private final boolean hasPrevious;
    private final boolean hasNext;
    private final long pageNumber;
    private PageMetadata<K, V> nextPageMetadata;
    private PageMetadata<K, V> previousPageMetadata;
    private final long totalRows;
    private final long resultFrom;
    private final long resultTo;

    private final List<Row<K, V>> rows = new ArrayList<Row<K, V>>();
    private List<K> keys = null;
    private List<V> values = null;
    private List<Document> docs = null;

    ViewResponseImpl(ViewQueryParameters<K, V> initialQueryParameters, JsonObject response,
                     PageMetadata<K, V> pageMetadata) {
        this.initialQueryParameters = initialQueryParameters;

        PageMetadata.PagingDirection thisPageDirection;
        if (pageMetadata == null) {
            previousPageMetadata = null;
            pageNumber = 1l;
            //from a first page we can only page FORWARD
            thisPageDirection = PageMetadata.PagingDirection.FORWARD;
        } else {
            this.pageNumber = pageMetadata.pageNumber;
            thisPageDirection = pageMetadata.direction;
        }

        //build the rows from the response
        JsonArray rowsArray = response.getAsJsonArray("rows");
        if (rowsArray != null) {
            for (JsonElement row : rowsArray) {
                rows.add(fromJson(row));
            }
        }
        int resultRows = rows.size();

        JsonElement totalRowsElement = response.get("total_rows");
        if (totalRowsElement != null) {
            totalRows = totalRowsElement.getAsLong();
        } else {
            //if there is no total rows element, use the rows size
            totalRows = rows.size();
        }

        Long rowsPerPage = (initialQueryParameters.getRowsPerPage() != null) ?
                initialQueryParameters.getRowsPerPage().longValue() : null;

        // We expect limit = rowsPerPage + 1 results, if we have rowsPerPage or less,
        // or if rowsPerPage wasn't set then we are on the last page.
        hasNext = (rowsPerPage != null) ? (resultRows > rowsPerPage) : false;

        if (PageMetadata.PagingDirection.BACKWARD == thisPageDirection) {
            //Result needs reversing because to implement backward paging the view reading
            // order is reversed
            Collections.reverse(rows);
        }

        //set previous page links if not the first page
        if (this.pageNumber > 1) {
            hasPrevious = true;
            // Construct the previous page metadata (i.e. paging backward)
            // Decrement the page number by 1
            // The startKey of this page is also the start key of the previous page, but using a
            // descending lookup indicated by the paging direction.
            previousPageMetadata = new PageMetadata<K, V>(PageMetadata.PagingDirection
                    .BACKWARD, this.pageNumber - 1l, PageMetadata.reversePaginationQueryParameters
                    (initialQueryParameters, rows.get(0).getKey(), rows.get(0).getId()));
        } else {
            hasPrevious = false;
        }

        // If we are not on the last page, we need to use the last
        // result as the start key for the next page and therefore
        // we don't return it to the user.
        // If we are on the last page, the final row should be returned
        // to the user.
        int lastIndex = resultRows - 1;
        if (hasNext) {
            // Construct the next page metadata (i.e. paging forward)
            // Increment the page number by 1
            // The last element is the start of the next page so use the key and ID from that
            // element for creating the next page query parameters.
            nextPageMetadata = new PageMetadata<K, V>(PageMetadata.PagingDirection
                    .FORWARD, this.pageNumber + 1l, PageMetadata.forwardPaginationQueryParameters
                    (initialQueryParameters, rows.get(lastIndex).getKey(), rows.get(lastIndex)
                            .getId()));

            // The final element is the first element of the next page, so remove from the list that
            // will be returned.
            rows.remove(lastIndex);
        } else {
            nextPageMetadata = null;
        }

        // calculate paging display info
        if (rowsPerPage != null) {
            long offset = (this.pageNumber - 1) * rowsPerPage;
            resultFrom = offset + 1;
            resultTo = offset + (hasNext ? rowsPerPage : resultRows);
        } else {
            resultFrom = 1;
            resultTo = totalRows;
        }
    }

    @Override
    public List<Row<K, V>> getRows() {
        return Collections.unmodifiableList(rows);
    }

    @Override
    public List<K> getKeys() {
        if (keys == null) {
            populateKV();
        }
        return keys;
    }

    @Override
    public List<V> getValues() {
        if (values == null) {
            populateKV();
        }
        return values;
    }

    @Override
    public List<Document> getDocs() {
        if (initialQueryParameters.getIncludeDocs()) {
            return internalGetDocs();
        } else {
            throw new IllegalStateException("Cannot getDocs() when include_docs is false.");
        }
    }

    protected List<Document> internalGetDocs() {
        if (docs == null) {
            docs = new ArrayList<Document>();
            for (Row row : getRows()) {
                Document doc = row.getDocument();
                if(doc != null) {
                    docs.add(doc);
                }
            }
        }
        return docs;
    }

    @Override
    public <D> List<D> getDocsAs(Class<D> docType) {
        if (initialQueryParameters.getIncludeDocs()) {
            List<D> documents = new ArrayList<D>();
            for (Row<K, V> row : getRows()) {
                documents.add(row.getDocumentAsType(docType));
            }
            return documents;
        } else {
            throw new IllegalStateException("Cannot getDocs() when include_docs is false.");
        }
    }

    @Override
    public boolean hasNextPage() {
        return hasNext;
    }

    @Override
    public boolean hasPreviousPage() {
        return hasPrevious;
    }

    @Override
    public ViewResponse<K, V> nextPage() throws IOException {
        if (hasNext) {
            JsonObject response = ViewRequester.getResponseAsJson(nextPageMetadata
                    .pageRequestParameters);
            return new ViewResponseImpl<K, V>(initialQueryParameters, response, nextPageMetadata);
        } else {
            return null;
        }
    }

    @Override
    public ViewResponse<K, V> previousPage() throws IOException {
        if (hasPrevious) {
            JsonObject response = ViewRequester.getResponseAsJson(previousPageMetadata
                    .pageRequestParameters);
            return new ViewResponseImpl<K, V>(initialQueryParameters, response,
                    previousPageMetadata);
        } else {
            return null;
        }
    }

    @Override
    public String getNextPageToken() {
        if (hasNext) {
            return PaginationToken.tokenize(nextPageMetadata);
        }
        return null;
    }

    @Override
    public String getPreviousPageToken() {
        if (hasPrevious) {
            return PaginationToken.tokenize(previousPageMetadata);
        }
        return null;
    }

    @Override
    public Long getPageNumber() {
        return this.pageNumber;
    }

    @Override
    public Long getFirstRowCount() {
        return resultFrom;
    }

    @Override
    public Long getLastRowCount() {
        return resultTo;
    }

    @Override
    public Long getTotalRowCount() {
        return totalRows;
    }

    @Override
    public Iterator<ViewResponse<K, V>> iterator() {
        return new Iterator<ViewResponse<K, V>>() {

            //initially true to allow next() to get the first element
            boolean hasNext = true;
            //hold a ref to the previous page so we don't have to make the GET for the next page
            // until next() is called
            ViewResponse<K, V> previousPage = null;

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public ViewResponse<K, V> next() {
                try {
                    if (hasNext) {
                        ViewResponse<K, V> pageToReturn = (previousPage == null) ?
                                ViewResponseImpl.this : previousPage.nextPage();
                        hasNext = pageToReturn.hasNextPage();
                        previousPage = pageToReturn;
                        return pageToReturn;
                    } else {
                        throw new NoSuchElementException("No more pages");
                    }
                } catch (IOException e) {
                    //iterators can't throw a checked exception, so wrap in a runtime
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private void populateKV() {
        keys = new ArrayList<K>();
        values = new ArrayList<V>();
        for (Row<K, V> row : getRows()) {
            keys.add(row.getKey());
            values.add(row.getValue());
        }
    }

    protected Row fromJson(JsonElement row) {
        return new RowImpl<K, V>(initialQueryParameters, row);
    }
}
