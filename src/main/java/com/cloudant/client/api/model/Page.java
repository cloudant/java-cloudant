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

package com.cloudant.client.api.model;

import com.cloudant.client.org.lightcouch.View;

import java.util.List;

/**
 * Holds data of a page as result of a view pagination query.
 *
 * @author Ganesh K Choudhary
 * @see View#queryPage(int, String, Class)
 * @since 0.0.1
 */
public class Page<T> {
    private com.cloudant.client.org.lightcouch.Page<T> page;

    public Page(com.cloudant.client.org.lightcouch.Page<T> page) {
        this.page = page;
    }

    public boolean isHasPrevious() {
        return page.isHasPrevious();
    }

    public boolean isHasNext() {
        return page.isHasNext();
    }

    public List<T> getResultList() {
        return page.getResultList();
    }

    public long getTotalResults() {
        return page.getTotalResults();
    }

    public int getResultFrom() {
        return page.getResultFrom();
    }

    public int getResultTo() {
        return page.getResultTo();
    }

    public int getPageNumber() {
        return page.getPageNumber();
    }

    public String getNextParam() {
        return page.getNextParam();
    }

    public String getPreviousParam() {
        return page.getPreviousParam();
    }

    /**
     * Setter methods on {@link Page} should not be called by applications.
     * These parameters are controlled internally.
     */
    @Deprecated
    public void setHasPrevious(boolean isHasPrevious) {
        page.setHasPrevious(isHasPrevious);
    }

    /**
     * Setter methods on {@link Page} should not be called by applications.
     * These parameters are controlled internally.
     */
    @Deprecated
    public void setHasNext(boolean isHasNext) {
        page.setHasNext(isHasNext);
    }

    /**
     * Setter methods on {@link Page} should not be called by applications.
     * These parameters are controlled internally.
     */
    @Deprecated
    public void setResultList(List<T> resultList) {
        page.setResultList(resultList);
    }

    /**
     * Setter methods on {@link Page} should not be called by applications.
     * These parameters are controlled internally.
     */
    @Deprecated
    public void setTotalResults(long totalResults) {
        page.setTotalResults(totalResults);
    }

    /**
     * Setter methods on {@link Page} should not be called by applications.
     * These parameters are controlled internally.
     */
    @Deprecated
    public void setResultFrom(int resultFrom) {
        page.setResultFrom(resultFrom);
    }

    /**
     * Setter methods on {@link Page} should not be called by applications.
     * These parameters are controlled internally.
     */
    @Deprecated
    public void setResultTo(int resultTo) {
        page.setResultTo(resultTo);
    }

    /**
     * Setter methods on {@link Page} should not be called by applications.
     * These parameters are controlled internally.
     */
    @Deprecated
    public void setPageNumber(int pageNumber) {
        page.setPageNumber(pageNumber);
    }

    /**
     * Setter methods on {@link Page} should not be called by applications.
     * These parameters are controlled internally.
     */
    @Deprecated
    public void setNextParam(String nextParam) {
        page.setNextParam(nextParam);
    }

    /**
     * Setter methods on {@link Page} should not be called by applications.
     * These parameters are controlled internally.
     */
    @Deprecated
    public void setPreviousParam(String previousParam) {
        page.setPreviousParam(previousParam);
    }


}
