package org.lightcouch;

/**
 * Thrown when a conflict is detected during a save or update.
 * @author Ahmed Yehia
 */
public class DocumentConflictException extends CouchDbException {
	
	private static final long serialVersionUID = 1L;

	public DocumentConflictException(String message) {
		super(message);
	}
	
	public DocumentConflictException(Throwable cause) {
		super(cause);
	}

	public DocumentConflictException(String message, Throwable cause) {
		super(message, cause);
	}
}
