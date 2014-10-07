package com.cloudant;

import java.util.List;

import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbDesign;
import org.lightcouch.DesignDocument;
import org.lightcouch.Response;

public class CloudantDbDesign {
	private CouchDbDesign couchDbDesign ;
	
	CloudantDbDesign(CouchDbDesign couchDbDesign ){
		this.couchDbDesign = couchDbDesign ;
	}

	/**
	 * Synchronizes a design document to the Database.
	 * <p>This method will first try to find a document in the database with the same id
	 * as the given document, if it is not found then the given document will be saved to the database.
	 * <p>If the document was found in the database, it will be compared with the given document using
	 *  {@code equals()}. If both documents are not equal, then the given document will be saved to the 
	 *  database and updates the existing document.
	 * @param document The design document to synchronize
	 * @return {@link Response} as a result of a document save or update, or returns {@code null} if no 
	 * action was taken and the document in the database is up-to-date with the given document.
	 */
	public Response synchronizeWithDb(DesignDocument document) {
		return couchDbDesign.synchronizeWithDb(document);
	}

	/**
	 * Synchronize all design documents on desk to the database.
	 * @see #synchronizeWithDb(DesignDocument)
	 * @see CouchDbClient#syncDesignDocsWithDb()
	 */
	public void synchronizeAllWithDb() {
		couchDbDesign.synchronizeAllWithDb();
	}

	/**
	 * Gets a design document from the database.
	 * @param id The document id
	 * @return {@link DesignDocument}
	 */
	public DesignDocument getFromDb(String id) {
		return couchDbDesign.getFromDb(id);
	}

	/**
	 * Gets a design document from the database.
	 * @param id The document id
	 * @param rev The document revision
	 * @return {@link DesignDocument}
	 */
	public DesignDocument getFromDb(String id, String rev) {
		return couchDbDesign.getFromDb(id, rev);
	}

	/**
	 * Gets all design documents from desk.
	 * @see #getFromDesk(String)
	 */
	public List<DesignDocument> getAllFromDesk() {
		return couchDbDesign.getAllFromDesk();
	}

	/**
	 * Gets a design document from desk.
	 * @param id The document id to get.
	 * @return {@link DesignDocument}
	 */
	public DesignDocument getFromDesk(String id) {
		return couchDbDesign.getFromDesk(id);
	}

	public String readContent(List<String> elements, String rootPath,
			String element) {
		return couchDbDesign.readContent(elements, rootPath, element);
	}
	
	
	
}
