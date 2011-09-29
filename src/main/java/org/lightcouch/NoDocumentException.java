package org.lightcouch;

/**
 * Thrown when a requested document is not found.
 * @author Ahmed Yehia
 */
public class NoDocumentException extends CouchDbException {

	private static final long serialVersionUID = 1L;
	
	public NoDocumentException(String message) {
		super(message);
	}
	
	public NoDocumentException(Throwable cause) {
		super(cause);
	}
	
	public NoDocumentException(String message, Throwable cause) {
		super(message, cause);
	}
}
