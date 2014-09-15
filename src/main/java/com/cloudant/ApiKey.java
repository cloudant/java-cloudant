package com.cloudant;

/**
 * Encapsulates a ApiKey response from Cloudant
 * @since 0.0.1
 * @author Mario Briggs
 *
 */
public class ApiKey {

	//@SerializedName("db_name")
	private String key;
	//@SerializedName("doc_count")
	private String password;
	

	/**
	 * Return the Apikey
	 * @return String
	 */
	public String getKey() {
		return key;
	}


	/**
	 * Return the password associated with the ApiKey
	 * @return String
	 */
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
