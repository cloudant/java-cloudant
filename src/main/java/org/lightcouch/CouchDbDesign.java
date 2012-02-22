/*
 * Copyright (C) 2011 Ahmed Yehia (ahmed.yehia.m@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lightcouch;

import static org.lightcouch.CouchDbUtil.*;
import static org.lightcouch.URIBuilder.builder;

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
 * Provides methods to create and save CouchDB design documents. 
 * <h3>Usage Example:</h3>
 * <pre>
 * DesignDocument exampleDoc = dbClient.design().getFromDesk("example");
 * Response response = dbClient.design().synchronizeWithDb(exampleDoc);
 * DesignDocument documentFromDb = dbClient.design().getFromDb("_design/example");
 * </pre>
 * @see DesignDocument
 * @author Ahmed Yehia
 */
public class CouchDbDesign {
	
	private static final String DESIGN_DOCS_DIR = "design-docs";
	private static final String JAVASCRIPT      = "javascript";
	private static final String DESIGN_PREFIX   = "_design/";
	private static final String VALIDATE_DOC    = "validate_doc_update";
	private static final String VIEWS           = "views";
	private static final String FILTERS         = "filters";
	private static final String SHOWS           = "shows";
	private static final String LISTS           = "lists";
	private static final String MAP_JS          = "map.js";
	private static final String REDUCE_JS       = "reduce.js";
	
	private CouchDbClient dbc;
	
	CouchDbDesign(CouchDbClient dbc) {
		this.dbc = dbc;
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
		assertNotEmpty(document, "Document");
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
	 * @see #synchronizeWithDb(DesignDocument)
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
		assertNotEmpty(id, "id");
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
		assertNotEmpty(id, "id");
		assertNotEmpty(id, "rev");
		URI uri = builder(dbc.getDBUri()).path(id).query("rev", rev).build();
		return dbc.get(uri, DesignDocument.class);
	}
	
	/**
	 * Gets all design documents from desk.
	 * @see #getFromDesk(String)
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
				throw new FileNotFoundException("Design docs directory not found");
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		DesignDocument dd = new DesignDocument();
		List<String> elements = Arrays.asList(designDoc.list());
		if(elements.contains(VALIDATE_DOC)) { // validate_doc_update
			File validateDir = new File(designDoc, VALIDATE_DOC);
			String[] validateFunctions = validateDir.list();
			if(validateFunctions.length != 1) {
				throw new IllegalArgumentException("Expecting exactly one validate_doc_update function file");
			}
			File validateFile = new File(validateDir, validateFunctions[0]);
			dd.setValidateDocUpdate(readFile(validateFile));
		} // /validate_doc_update
		Map<String, MapReduce> views = null;
		if(elements.contains(VIEWS)) { // views
			File viewsRootDir = new File(designDoc, VIEWS);
			views = new HashMap<String, MapReduce>();
			for (String viewDirName : viewsRootDir.list()) { // views sub-dirs
				MapReduce mr = dd.new MapReduce();
				File viewDir = new File(viewsRootDir, viewDirName);
				for (String fileName : viewDir.list()) { // view files
					String def = readFile(new File(viewDir, fileName));
					if(MAP_JS.equals(fileName))
						mr.setMap(def);
					else if(REDUCE_JS.equals(fileName))
						mr.setReduce(def);
				} // /foreach view files
				views.put(viewDirName, mr);
			} // /foreach views sub-dirs
		} // /views
		dd.setId(DESIGN_PREFIX + id); 
		dd.setLanguage(JAVASCRIPT);
		dd.setViews(views);
		dd.setFilters(populateMap(designDoc, elements, FILTERS));
		dd.setShows(populateMap(designDoc, elements, SHOWS));
		dd.setLists(populateMap(designDoc, elements, LISTS));
		return dd;
	}
	
	private Map<String, String> populateMap(File designDoc, List<String> elements, String element) {
		Map<String, String> functionsMap = null;
		if(elements.contains(element)) {
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
