package org.lightcouch;

import java.util.List;

/**
 * Holds data of a page as result of a view pagination query. 
 * @see View#queryPage(int, String, Class)
 * @param <T> Object type T
 * 
 * @author Ahmed Yehia
 */
public class Page<T> {
	private boolean isHasPrevious;
	private boolean isHasNext;
	private List<T> resultList;
	private long totalResults;
	private int resultFrom;
	private int resultTo;
	private int pageNumber;
	private String nextParam;
	private String previousParam;

	public boolean isHasPrevious() {
		return isHasPrevious;
	}

	public boolean isHasNext() {
		return isHasNext;
	}

	public List<T> getResultList() {
		return resultList;
	}

	public long getTotalResults() {
		return totalResults;
	}

	public int getResultFrom() {
		return resultFrom;
	}

	public int getResultTo() {
		return resultTo;
	}

	public int getPageNumber() {
		return pageNumber;
	}

	public String getNextParam() {
		return nextParam;
	}

	public String getPreviousParam() {
		return previousParam;
	}

	public void setHasPrevious(boolean isHasPrevious) {
		this.isHasPrevious = isHasPrevious;
	}

	public void setHasNext(boolean isHasNext) {
		this.isHasNext = isHasNext;
	}

	public void setResultList(List<T> resultList) {
		this.resultList = resultList;
	}

	public void setTotalResults(long totalResults) {
		this.totalResults = totalResults;
	}

	public void setResultFrom(int resultFrom) {
		this.resultFrom = resultFrom;
	}

	public void setResultTo(int resultTo) {
		this.resultTo = resultTo;
	}

	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	public void setNextParam(String nextParam) {
		this.nextParam = nextParam;
	}

	public void setPreviousParam(String previousParam) {
		this.previousParam = previousParam;
	}
}
