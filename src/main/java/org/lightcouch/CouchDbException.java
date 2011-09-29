package org.lightcouch;


/**
 * Base runtime exception class.
 * @see {@link NoDocumentException}, {@link DocumentConflictException}
 * @author Ahmed Yehia
 */
public class CouchDbException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CouchDbException(String message) {
		super(message);
	}
	
	public CouchDbException(Throwable cause) {
		super(cause);
	}
	
	public CouchDbException(String message, Throwable cause) {
		super(message, cause);
	}
}
