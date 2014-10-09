package com.cloudant;

import java.util.List;

import org.lightcouch.View;

/**
 * Holds data of a page as result of a view pagination query. 
 * @see View#queryPage(int, String, Class)
 * @since 0.0.1
 * @author Ganesh K Choudhary
 */
public class Page<T> {
	private org.lightcouch.Page<T> page ;
	
	Page(org.lightcouch.Page<T> page){
		this.page = page ;
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

	public void setHasPrevious(boolean isHasPrevious) {
		page.setHasPrevious(isHasPrevious);
	}

	public void setHasNext(boolean isHasNext) {
		page.setHasNext(isHasNext);
	}

	public void setResultList(List<T> resultList) {
		page.setResultList(resultList);
	}

	public void setTotalResults(long totalResults) {
		page.setTotalResults(totalResults);
	}

	public void setResultFrom(int resultFrom) {
		page.setResultFrom(resultFrom);
	}

	public void setResultTo(int resultTo) {
		page.setResultTo(resultTo);
	}

	public void setPageNumber(int pageNumber) {
		page.setPageNumber(pageNumber);
	}

	public void setNextParam(String nextParam) {
		page.setNextParam(nextParam);
	}

	public void setPreviousParam(String previousParam) {
		page.setPreviousParam(previousParam);
	}
	
	
	
}
