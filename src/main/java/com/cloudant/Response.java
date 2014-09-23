package com.cloudant;

public class Response {
	private org.lightcouch.Response response ;

	public Response(){
		this.response = new org.lightcouch.Response();
	}
	
	Response(org.lightcouch.Response response){
		this.response = response ;
	}
	
	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return response.equals(obj);
	}

	/**
	 * @return
	 * @see org.lightcouch.Response#getId()
	 */
	public String getId() {
		return response.getId();
	}

	/**
	 * @return
	 * @see org.lightcouch.Response#getRev()
	 */
	public String getRev() {
		return response.getRev();
	}

	/**
	 * @return
	 * @see org.lightcouch.Response#getError()
	 */
	public String getError() {
		return response.getError();
	}

	/**
	 * @return
	 * @see org.lightcouch.Response#getReason()
	 */
	public String getReason() {
		return response.getReason();
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return response.hashCode();
	}

	/**
	 * @return
	 * @see org.lightcouch.Response#toString()
	 */
	public String toString() {
		return response.toString();
	}
	
	
}
