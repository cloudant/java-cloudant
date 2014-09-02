package com.cloudant.tests;

public class Movie {

	private String Movie_name;
	private int Movie_year;
	
	/**
	 * @return the movie_name
	 */
	public String getMovie_name() {
		return Movie_name;
	}
	/**
	 * @return the movie_year
	 */
	public int getMovie_year() {
		return Movie_year;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return  Movie_name + ", " + Movie_year;
	}
	
	
	
}
