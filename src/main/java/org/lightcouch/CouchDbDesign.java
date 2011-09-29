package org.lightcouch;

import static org.lightcouch.CouchDbUtil.*;
import static org.lightcouch.URIBuilder.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lightcouch.DesignDocument.MapReduce;

/**
 * Provides access for the creation and saving of CouchDB design documents. 
 * <h3>Usage Example:</h3>
 * <pre>
 * DesignDocument exampleDoc = dbClient.design().getFromDesk("example");
 * Response response = dbClient.design().synchronizeWithDb(exampleDoc);
 * DesignDocument documentFromDb = dbClient.design().getFromDb("_design/example");
 * </pre>
 * @author Ahmed Yehia
 */
public class CouchDbDesign {
	
	private static final String DESIGN_DOCS_DIR = "design-docs";
	
	private CouchDbClient dbc;
	
	CouchDbDesign(CouchDbClient dbc) {
		this.dbc = dbc;
	}
	
	/**
	 * Synchronizes a design document to the Database.
	 * <p>This method will first try to find a document in the database with the same id
	 * as the given document, if it is not found then the given document will be saved to the database.
	 * <p>If the document was found in the database, it will be compared with the given document using
	 *  {@code equals()}. If both documents are not equal, then the the given document will be saved to the 
	 *  database and updates the existing document.
	 * @param document The design document to synchronize
	 * @return {@link Response} as a result of a document save or update, or returns {@code null} if no 
	 * action was taken and the document in the database is up-to-date with the given document.
	 */
	public Response synchronizeWithDb(DesignDocument document) {
		assertNotEmpty(document, "Design Document");
		DesignDocument documentFromDb = null;
		try {
			documentFromDb = getFromDb(document.getId());
		} catch (NoDocumentException e) {
			return dbc.save(document);
		}
		if(!document.equals(documentFromDb)) { 
			document.setRevision(documentFromDb.getRevision());
			return dbc.update(document);
		}
		return null;
	}
	
	/**
	 * Synchronize all design documents from desk to the database.
	 * @see #synchronizeDesignDocWithDb
	 */
	public void synchronizeAllWithDb() {
		List<DesignDocument> documents = getAllFromDesk();
		for (DesignDocument dd : documents) {
			synchronizeWithDb(dd);
		}
	}
	
	/**
	 * Gets a design document from the database.
	 * @param id The document id
	 * @return {@link DesignDocument}
	 */
	public DesignDocument getFromDb(String id) {
		assertNotEmpty(id, "Document id");
		URI uri = builder(dbc.getDBUri()).path(id).build();
		return dbc.get(uri, DesignDocument.class);
	}
	
	/**
	 * Gets a design document from the database.
	 * @param id The document id
	 * @param rev The document revision
	 * @return {@link DesignDocument}
	 */
	public DesignDocument getFromDb(String id, String rev) {
		assertNotEmpty(id, "Document id");
		assertNotEmpty(id, "Document rev");
		URI uri = builder(dbc.getDBUri()).path(id).query("rev", rev).build();
		return dbc.get(uri, DesignDocument.class);
	}
	
	/**
	 * Gets all design documents from desk.
	 */
	public List<DesignDocument> getAllFromDesk() {
		File rootDir = null;
		try {
			rootDir = new File(getURL(DESIGN_DOCS_DIR).toURI());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
		List<DesignDocument> designDocsList = new ArrayList<DesignDocument>();
		for (String docName : rootDir.list()) {
			designDocsList.add(getFromDesk(docName));
		} 
		return designDocsList;
	}
	
	/**
	 * Gets a design document from desk.
	 * @param id The document id to get.
	 * @return {@link DesignDocument}
	 */
	public DesignDocument getFromDesk(String id) {
		assertNotEmpty(id, "id");
		File designDoc = null;
		try {
			designDoc = new File(new File(getURL(DESIGN_DOCS_DIR).toURI()), id);
			if(!designDoc.exists()) {
				throw new FileNotFoundException();
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		DesignDocument dd = new DesignDocument();
		Map<String, String> filters = null;
		Map<String, String> lists = null;
		Map<String, String> shows = null;
		Map<String, MapReduce> views = null;
		String[] elements = designDoc.list();
		lists   = populateFunctions(lists, designDoc, elements, "lists");
		filters = populateFunctions(lists, designDoc, elements, "filters");
		shows   = populateFunctions(lists, designDoc, elements, "shows");
		// validate_doc_update functions
		if(Arrays.asList(elements).contains("validate_doc_update")) {
			File validateDir = new File(designDoc, "validate_doc_update");
			String[] validateFunctions = validateDir.list();
			if(validateFunctions.length != 1) {
				throw new IllegalArgumentException("Expecting exactly one validate_doc_update function file");
			}
			File validateFile = new File(validateDir, validateFunctions[0]);
			dd.setValidateDocUpdate(readFile(validateFile));
		} // /validate_doc_update
		if(Arrays.asList(elements).contains("views")) { // view functions
			File viewsRootDir = new File(designDoc, "views");
			views = new HashMap<String, MapReduce>();
			for (String viewDirName : viewsRootDir.list()) { // view dirs
				MapReduce mr = dd.new MapReduce();
				File viewDir = new File(viewsRootDir, viewDirName);
				String map = null;
				String reduce = null;
				for (String mapReduceFileName : viewDir.list()) { // view sub dirs
					if(mapReduceFileName.equals("map.js")) {
						map = readFile(new File(viewDir, mapReduceFileName));
						mr.setMap(map);
					} else if(mapReduceFileName.equals("reduce.js")) {
						reduce = readFile(new File(viewDir, mapReduceFileName));
						mr.setReduce(reduce);
					}
				} // /foreach view sub dirs
				views.put(viewDirName, mr);
			} // /foreach view dirs
		} // /view functions
		dd.setId("_design/" + id); 
		dd.setLanguage("javascript");
		dd.setViews(views);
		dd.setFilters(filters);
		dd.setShows(shows);
		dd.setLists(lists);
		return dd;
	}
	
	private Map<String, String> populateFunctions(Map<String, String> functionsMap,
			File designDoc, String[] elements, String element) {
		if(Arrays.asList(elements).contains(element)) {
			File functionsDir = new File(designDoc, element);
			functionsMap = new HashMap<String, String>();
			for (String functionFileName : functionsDir.list()) {
				File functionFile = new File(functionsDir, functionFileName);
				functionsMap.put(removeExtension(functionFileName), readFile(functionFile));
			}
		}
		return functionsMap;
	}
}
