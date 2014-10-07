package com.cloudant;

import org.lightcouch.DocumentConflictException;
import org.lightcouch.NoDocumentException;

/**
 * Base runtime exception class.
 * @see {@link NoDocumentException}, {@link DocumentConflictException}
 * @author Ganesh K Choudhary
 */
public class CloudantException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public CloudantException(String message) {
		super(message);
	}

	public CloudantException(Throwable cause) {
		super(cause);
	}

	public CloudantException(String message, Throwable cause) {
		super(message, cause);
	}

}
