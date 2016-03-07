/*
 * Copyright (c) 2015 IBM Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.cloudant.client.api;

import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.assertNotEmpty;

import com.cloudant.client.api.model.DesignDocument;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.cloudant.client.org.lightcouch.internal.CouchDbUtil;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides API to work with design documents.
 * <h3>Usage Example:</h3>
 * <pre>
 * // read from javascript design document file
 * {@code
 * DesignDocument design = DesignDocumentManager.fromFile("../example.js");
 * }
 *
 * // read from directory of design documents
 * {@code
 * List<DesignDocument> design =
 *                  DesignDocumentManager.fromDirectory("../design-files/");
 * }
 *
 * // sync with the database
 * {@code
 * db.getDesignDocumentManager().put(designDoc);
 * }
 *
 * // sync multiple design documents with the database
 * {@code
 * db.getDesignDocumentManager().put(listDesignDocuments);
 * }
 *
 * // read from the database
 * {@code
 * DesignDocument designDoc2 = db.getDesignDocumentManager().get("_design/example");
 * }
 * </pre>
 *
 * See {@link Database#getDesignDocumentManager()} to access the API.
 *
 * @see DesignDocument
 * @since 2.0.0
 */
public class DesignDocumentManager {

    private Database db;

    DesignDocumentManager(Database db) {
        this.db = db;
    }

    /**
     * Synchronizes a design document to the Database.
     * <p>This method will first try to find a document in the database with the same id
     * as the given document, if it is not found then the given document will be saved to the
     * database.
     * <p>If the document was found in the database, it will be compared with the given document
     * using {@code equals()}. If both documents are not equal, then the given document will be
     * saved to the database and updates the existing document.
     *
     * @param document the design document to synchronize (optionally prefixed with "_design/")
     * @return {@link Response} as a result of a document save or update, or returns {@code null}
     * if no action was taken and the document in the database is up-to-date with the given
     * document.
     */
    public Response put(DesignDocument document) {
        CouchDbUtil.assertNotEmpty(document, "DesignDocument");
        DesignDocument documentFromDb;
        try {
            documentFromDb = get(document.getId());

        } catch (NoDocumentException e) {
            return db.save(document);
        }
        if (!document.equals(documentFromDb)) {
            document.setRevision(documentFromDb.getRevision());
            return db.update(document);
        }
        return null;
    }

    /**
     * Synchronize multiple design documents with the database.
     *
     * @param designDocs DesignDocument objects to put in the database
     * @see #put(DesignDocument)
     */
    public void put(DesignDocument... designDocs) {
        for (DesignDocument designDocument : designDocs) {
            put(designDocument);
        }
    }

    /**
     * Gets a design document from the database.
     *
     * @param id the design document id (optionally prefixed with "_design/")
     * @return {@link DesignDocument}
     */
    public DesignDocument get(String id) {
        assertNotEmpty(id, "id");
        return db.find(DesignDocument.class, id);
    }

    /**
     * Gets a design document using the id and revision from the database.
     *
     * @param id  the document id (optionally prefixed with "_design/")
     * @param rev the document revision
     * @return {@link DesignDocument}
     */
    public DesignDocument get(String id, String rev) {
        assertNotEmpty(id, "id");
        assertNotEmpty(id, "rev");
        return db.find(DesignDocument.class, id, rev);
    }

    /**
     * Removes a design document from the database.
     *
     * @param id the document id (optionally prefixed with "_design/")
     * @return {@link DesignDocument}
     */
    public Response remove(String id) {
        assertNotEmpty(id, "id");
        DesignDocument find = db.find(DesignDocument.class, id);
        return db.remove(find.getId(), find.getRevision());
    }

    /**
     * Removes a design document using the id and rev from the database.
     *
     * @param id  the document id
     * @param rev the document revision
     * @return {@link DesignDocument}
     */
    public Response remove(String id, String rev) {
        assertNotEmpty(id, "id");
        assertNotEmpty(id, "rev");
        return db.remove(id, rev);

    }

    /**
     * Removes a design document using DesignDocument object from the database.
     *
     * @param designDocument the design document object to be removed
     * @return {@link DesignDocument}
     */
    public Response remove(DesignDocument designDocument) {
        assertNotEmpty(designDocument, "DesignDocument");
        return db.remove(designDocument);
    }

    /**
     * Deserialize a directory of javascript design documents to a List of DesignDocument objects.
     *
     * @param directory the directory containing javascript files
     * @return {@link DesignDocument}
     * @throws FileNotFoundException if the file does not exist or cannot be read
     */
    public static List<DesignDocument> fromDirectory(File directory) throws FileNotFoundException {
        List<DesignDocument> designDocuments = new ArrayList<DesignDocument>();
        if (directory.isDirectory()) {
            Collection<File> files = FileUtils.listFiles(directory, null, true);
            for (File designDocFile : files) {
                designDocuments.add(fromFile(designDocFile));
            }
        } else {
            designDocuments.add(fromFile(directory));
        }
        return designDocuments;
    }

    /**
     * Deserialize a javascript design document file to a DesignDocument object.
     *
     * @param file the design document javascript file (UTF-8 encoded)
     * @return {@link DesignDocument}
     * @throws FileNotFoundException if the file does not exist or cannot be read
     */
    public static DesignDocument fromFile(File file) throws FileNotFoundException {
        assertNotEmpty(file, "Design js file");
        DesignDocument designDocument;
        Gson gson = new Gson();
        try {
            //Deserialize JS file contents into DesignDocument object
            designDocument = gson.fromJson(new InputStreamReader(new FileInputStream(file),
                    "UTF-8"), DesignDocument.class);

            return designDocument;
        } catch (UnsupportedEncodingException e) {
            //UTF-8 should be supported on all JVMs
            throw new RuntimeException(e);
        }
    }

}
