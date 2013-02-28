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
import static java.lang.String.format;
import static org.lightcouch.URIBuilder.builder;

import java.net.URI;
import java.util.ArrayList;
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
	private static final String UPDATES         = "updates";
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
	 * Synchronize all design documents on desk to the database.
	 * @see #synchronizeWithDb(DesignDocument)
	 * @see CouchDbClient#syncDesignDocsWithDb()
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
		List<DesignDocument> designDocsList = new ArrayList<DesignDocument>();
		for (String docName : listResources(format("%s/", DESIGN_DOCS_DIR))) {
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
		DesignDocument dd = new DesignDocument();
		String rootPath = format("%s/%s/", DESIGN_DOCS_DIR, id);
		List<String> elements = listResources(rootPath);
		if(elements == null) {
			throw new IllegalArgumentException("Design docs directory cannot be empty.");
		}
		
		if(elements.contains(VALIDATE_DOC)) { // validate_doc_update
			String validateDocPath = format("%s%s/", rootPath, VALIDATE_DOC);
			List<String> dirList = listResources(validateDocPath);
			for (String file : dirList) {
				String contents = readFile(format("/%s%s", validateDocPath, file));
				dd.setValidateDocUpdate(contents);
				break; // only one validate_doc_update file
			}
		} // /validate_doc_update
		Map<String, MapReduce> views = null;
		if(elements.contains(VIEWS)) { // views
			String viewsPath = format("%s%s/", rootPath, VIEWS);
			views = new HashMap<String, MapReduce>();
			for (String viewDirName : listResources(viewsPath)) { // views sub-dirs
				MapReduce mr = new MapReduce();
				String viewPath = format("%s%s/", viewsPath, viewDirName);
				List<String> dirList = listResources(viewPath);
				for (String fileName : dirList) { // view files
					String def = readFile(format("/%s%s", viewPath, fileName));
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
		dd.setFilters(populateMap(rootPath, elements, FILTERS));
		dd.setShows(populateMap(rootPath, elements, SHOWS));
		dd.setLists(populateMap(rootPath, elements, LISTS));
		dd.setUpdates(populateMap(rootPath, elements, UPDATES));
		return dd;
	}
	
	private Map<String, String> populateMap(String rootPath, List<String> elements, String element) {
		Map<String, String> functionsMap = null;
		if(elements.contains(element)) {
			functionsMap = new HashMap<String, String>();
			String path = format("%s%s/", rootPath, element);
			for (String fileName : listResources(path)) {
				String contents = readFile(format("/%s%s", path, fileName));
				functionsMap.put(removeExtension(fileName), contents);
			}
		}
		return functionsMap;
	}
}
