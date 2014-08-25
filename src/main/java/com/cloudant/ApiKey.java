package com.cloudant;


public class ApiKey {

	//@SerializedName("db_name")
	private String key;
	//@SerializedName("doc_count")
	private String password;
	

	public String getKey() {
		return key;
	}


	public String getPassword() {
		return password;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "key:" + key + " password:" + password;
	}
	
	
	
}
