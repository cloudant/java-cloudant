/*
 * Copyright (c) 2015, 2018 IBM Corp. All rights reserved.
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

package com.cloudant.client.api.views;

/**
 * Describes the parameters that can be set when building view requests.
 *
 * <P>
 * <a target="_blank" href="https://console.bluemix.net/docs/services/Cloudant/api/using_views.html#using-views">
 * Cloudant API reference
 * </a>
 * </P>
 *
 * @since 2.0.0
 */
public interface SettableViewParameters {

    /**
     * Convenience constant defined as null to omit the value for a stale parameter, resulting in
     * the server default case, i.e. not allowing stale documents.
     * {@link com.cloudant.client.api.views.SettableViewParameters.Common#stale(String)}
     *
     * @since 2.5.0
     */
    String STALE_NO = null;
    /**
     * Constant for the value "ok" for use with
     * {@link com.cloudant.client.api.views.SettableViewParameters.Common#stale(String)}
     * <P>
     * ok: allow stale views
     * </P>
     */
    String STALE_OK = "ok";
    /**
     * Constant for the value "update_after" for use with
     * {@link com.cloudant.client.api.views.SettableViewParameters.Common#stale(String)}
     * <P>
     * update_after: Allow stale views, but update them immediately after the request.
     * </P>
     */
    String STALE_UPDATE_AFTER = "update_after";

    /**
     * Constant for the value "false" for use with
     * {@link com.cloudant.client.api.views.SettableViewParameters.Common#update(String)}
     * <P>
     * false: Return results before updating the view.
     * </P>
     *
     * @since 2.13.0
     */
    String UPDATE_FALSE = Boolean.FALSE.toString();

    /**
     * Constant for the value "true" for use with
     * {@link com.cloudant.client.api.views.SettableViewParameters.Common#update(String)}
     * <P>
     * true: Return results after updating the view.
     * </P>
     *
     * @since 2.13.0
     */
    String UPDATE_TRUE = Boolean.TRUE.toString();

    /**
     * Constant for the value "lazy" for use with
     * {@link com.cloudant.client.api.views.SettableViewParameters.Common#update(String)}
     * <P>
     * lazy: Return the view results without waiting for an update, but update them immediately
     * after the request.
     * </P>
     *
     * @since 2.13.0
     */
    String UPDATE_LAZY = "lazy";

    /**
     * Setters for parameters that are common to all view requests.
     *
     * @param <K>  the type of the key emitted by the view
     * @param <RB> the type of the request builder returned after each set operation
     * @since 2.0.0
     */
    interface Common<K, RB extends RequestBuilder> {
        /**
         * Return the documents in ‘descending by key’ order.
         *
         * @param descending {@code true} to return the documents in "descending by key" order,
         *                   default {@code false}
         * @return the builder to compose additional parameters or build the request
         * @since 2.0.0
         */
        RB descending(boolean descending);

        /**
         * Stop returning records when the specified key is reached.
         *
         * @param endkey of the type emitted by the view
         * @return the builder to compose additional parameters or build the request
         * @since 2.0.0
         */
        RB endKey(K endkey);

        /**
         * Stop returning records when the specified document ID is reached.
         * <P>
         * Used to distinguish between records with the same endkey.
         * </P>
         *
         * @param endkey_docid endkey document ID
         * @return the builder to compose additional parameters or build the request
         * @since 2.0.0
         */
        RB endKeyDocId(String endkey_docid);

        /**
         * Include the full content of the documents in the response.
         * <P>
         * Note that using include_docs=true might have
         * <a target="_blank"
         * href="https://console.bluemix.net/docs/services/Cloudant/api/using_views.html#multi-document-fetching">
         * performance implications.</a>
         * </P>
         *
         * @param include_docs {@code true} to return the full content, default {@code false}
         * @return the builder to compose additional parameters or build the request
         * @since 2.0.0
         */
        public RB includeDocs(boolean include_docs);

        /**
         * Include rows with the specified endkey.
         *
         * @param inclusive_end {@code false} to exclude, default {@code true}
         * @return the builder to compose additional parameters or build the request
         * @since 2.0.0
         */
        RB inclusiveEnd(boolean inclusive_end);

        /**
         * Return only documents that match the specified key or keys.
         *
         * @param keys one or more keys of the type emitted by the view
         * @return the builder to compose additional parameters or build the request
         * @since 2.0.0
         */
        RB keys(K... keys);

        /**
         * <p>
         * Determine whether the view should be returned from a "stable" set of shards.
         * </p>
         *
         * @param stable string indicating stable view behaviour
         * @return the builder to compose additional parameters or build the request
         * @since 2.13.0
         */
        RB stable(boolean stable);

        /**
         * Allow the results from a stale view to be used. This makes the request return
         * immediately, even if the view has not been completely built yet.
         * <P>If this parameter is not given, a response is returned only after the view has been
         * built.
         * </P>
         * <P>
         * See:
         * </P>
         * <UL>
         * <LI>{@link SettableViewParameters#STALE_OK}</LI>
         * <LI>{@link SettableViewParameters#STALE_UPDATE_AFTER}</LI>
         * </UL>
         *
         * @param stale string indicating stale view behaviour
         * @return the builder to compose additional parameters or build the request
         * @since 2.0.0
         * @deprecated use {@link #stable(boolean)} and {@link #update(String)} instead
         */
        RB stale(String stale);

        /**
         * Return records starting with the specified key.
         *
         * @param startkey of the type emitted by the view
         * @return the builder to compose additional parameters or build the request
         * @since 2.0.0
         */
        RB startKey(K startkey);

        /**
         * Return records starting with the specified document ID.
         * <P>
         * Used to distinguish between records with the same endkey.
         * </P>
         *
         * @param startkey_docid startkey document ID
         * @return the builder to compose additional parameters or build the request
         * @since 2.0.0
         */
        RB startKeyDocId(String startkey_docid);

        /**
         * <p>
         * Determine whether the view in question should be updated prior to or after responding
         * to the user.
         * </p>
         * <p>
         * See:
         * </p>
         * <ul>
         * <li>{@link SettableViewParameters#UPDATE_FALSE}</li>
         * <li>{@link SettableViewParameters#UPDATE_TRUE}</li>
         * <li>{@link SettableViewParameters#UPDATE_LAZY}</li>
         * </ul>
         *
         * @param update string indicating update view behaviour
         * @return the builder to compose additional parameters or build the request
         * @since 2.13.0
         */

        RB update(String update);
    }

    /**
     * Setters for parameters applicable to views that have reduce
     *
     * @param <K>  the type of the key emitted by the view
     * @param <RB> the type of the request builder returned after each set operation
     */
    interface Reduceable<K, RB extends RequestBuilder> {

        /**
         * Use the reduce function of the view.
         * <P>
         * Note that although the default is {@code true} if a view does not have a reduce
         * function this parameter will have no effect.
         * </P>
         *
         * @param reduce {@code false} to not use reduce, default {@code true}
         * @return the builder to compose additional parameters or build the request
         * @since 2.0.0
         */
        RB reduce(boolean reduce);

        /**
         * Group the results to a group or single row when using a reduce function.
         *
         * @param group {@code true} to group, default {@code false},
         * @return the builder to compose additional parameters or build the request
         * @since 2.0.0
         */
        RB group(boolean group);

        /**
         * Group reduce results for the specified number of array fields. Only applicable when
         * using a complex (JSON array) key.
         *
         * @param group_level number of array fields to group
         * @return the builder to compose additional parameters or build the request
         * @since 2.0.0
         */
        RB groupLevel(int group_level);
    }

    /**
     * Setters for parameters only available to unpaginated requests.
     *
     * @param <K>  the type of the key emitted by the view
     * @param <RB> the type of the request builder returned after each set operation
     * @since 2.0.0
     */
    interface Unpaginated<K, RB extends RequestBuilder> extends Common<K, RB> {

        /**
         * Limits the number of returned rows to the specified count.
         * <P>
         * Do not use skip/limit for pagination, instead use
         * {@link com.cloudant.client.api.views.ViewRequestBuilder#newPaginatedRequest(Key.Type,
         * Class)}
         * .
         * </P>
         *
         * @param limit the number of results to limit to
         * @return the builder to compose additional parameters or build the request
         * @since 2.0.0
         */
        RB limit(int limit);

        /**
         * Skips the specified number of rows from the start.
         * <P>
         * Do not use skip/limit for pagination, instead use
         * {@link com.cloudant.client.api.views.ViewRequestBuilder#newPaginatedRequest(Key.Type,
         * Class)}
         * .
         * </P>
         *
         * @param skip the number of results to skip
         * @return the builder to compose additional parameters or build the request
         * @since 2.0.0
         */
        RB skip(long skip);
    }

    /**
     * Setters for parameters only available to paginated requests.
     *
     * @param <K>  the type of the key emitted by the view
     * @param <RB> the type of the request builder returned after each set operation
     * @since 2.0.0
     */
    interface Paginated<K, RB extends RequestBuilder> extends Common<K, RB> {

        /**
         * Limits the number of returned rows to the specified number on each page.
         *
         * @param rowsPerPage the number of rows on each page; between 1 and {@link
         *                    Integer#MAX_VALUE}-1
         * @return the builder to compose additional parameters or build the request
         * @since 2.0.0
         */
        RB rowsPerPage(int rowsPerPage);
    }
}
