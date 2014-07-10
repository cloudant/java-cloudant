/*
 * Copyright (C) 2011 lightcouch.org
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

import static java.lang.String.format;
import static org.lightcouch.CouchDbUtil.assertNotEmpty;
import static org.lightcouch.CouchDbUtil.listResources;
import static org.lightcouch.CouchDbUtil.readFile;
import static org.lightcouch.CouchDbUtil.removeExtension;
import static org.lightcouch.URIBuilder.buildUri;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lightcouch.DesignDocument.MapReduce;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Provides API to work with design documents. 
 * <h3>Usage Example:</h3>
 * <pre>
 * // read from system files
 * DesignDocument design1 = dbClient.design().getFromDesk("example");
 * 
 * // sync with the database
 * dbClient.design().synchronizeWithDb(design1);
 * 
 * // sync all with the database
 * dbClient.syncDesignDocsWithDb();
 * 
 * // read from the database
 * DesignDocument design2 = dbClient.design().getFromDb("_design/example");
 * </pre>
 * @see {@link CouchDbClient#design() dbClient.design()} to access the API.
 * @see DesignDocument
 * @since 0.0.2
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
	private static final String REWRITES        = "rewrites";
	private static final String FULLTEXT        = "fulltext";
	private static final String INDEXES         = "indexes";
	private static final String MAP_JS          = "map.js";
	private static final String REDUCE_JS       = "reduce.js";
	
	private CouchDbClientBase dbc;
	
	CouchDbDesign(CouchDbClientBase dbc) {
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
		final URI uri = buildUri(dbc.getDBUri()).path(id).build();
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
		final URI uri = buildUri(dbc.getDBUri()).path(id).query("rev", rev).build();
		return dbc.get(uri, DesignDocument.class);
	}
	
	/**
	 * Gets all design documents from desk.
	 * @see #getFromDesk(String)
	 */
	public List<DesignDocument> getAllFromDesk() {
		final List<DesignDocument> designDocsList = new ArrayList<DesignDocument>();
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
		final DesignDocument dd = new DesignDocument();
		final String rootPath = format("%s/%s/", DESIGN_DOCS_DIR, id);
		final List<String> elements = listResources(rootPath);
		if(elements == null) {
			throw new IllegalArgumentException("Design docs directory cannot be empty.");
		}
		// Views
		Map<String, MapReduce> views = null;
		if(elements.contains(VIEWS)) { 
			views = new HashMap<String, MapReduce>();
			final String viewsPath = format("%s%s/", rootPath, VIEWS);
			for (String viewDirName : listResources(viewsPath)) { // views sub-dirs
				final MapReduce mr = new MapReduce();
				final String viewPath = format("%s%s/", viewsPath, viewDirName);
				final List<String> dirList = listResources(viewPath);
				for (String fileName : dirList) { // view files
					final String def = readFile(format("/%s%s", viewPath, fileName));
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
		dd.setValidateDocUpdate(readContent(elements, rootPath, VALIDATE_DOC));
		dd.setRewrites(dbc.getGson().fromJson(readContent(elements, rootPath, REWRITES), JsonArray.class));
		dd.setFulltext(dbc.getGson().fromJson(readContent(elements, rootPath, FULLTEXT), JsonObject.class));
		dd.setIndexes(dbc.getGson().fromJson(readContent(elements, rootPath, INDEXES), JsonObject.class));
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
	
	public String readContent(List<String> elements, String rootPath, String element) {
		if(elements.contains(element)) { 
			String path = format("%s%s/", rootPath, element);
			List<String> dirList = listResources(path);
			for (String file : dirList) {
				String contents = readFile(format("/%s%s", path, file));
				return contents;
			}
		} 
		return null;
	}
}
