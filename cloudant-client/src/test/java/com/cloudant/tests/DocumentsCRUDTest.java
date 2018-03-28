/*
 * Copyright (C) 2011 lightcouch.org
 * Copyright © 2015, 2018 IBM Corp. All rights reserved.
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
package com.cloudant.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudant.client.api.model.Params;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.org.lightcouch.DocumentConflictException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.base.TestWithDbPerClass;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiresDB
public class DocumentsCRUDTest extends TestWithDbPerClass {

    // Find

    @Test
    public void findById() {
        Response response = db.save(new Foo());
        Foo foo = db.find(Foo.class, response.getId());
        assertNotNull(foo);
    }

    @Test
    public void findByIdAndRev() {
        Response response = db.save(new Foo());
        Foo foo = db.find(Foo.class, response.getId(), response.getRev());
        assertNotNull(foo);
    }

    @Test
    public void findByIdContainSlash() {
        Response response = db.save(new Foo(generateUUID() + "/" + generateUUID()));
        Foo foo = db.find(Foo.class, response.getId());
        assertNotNull(foo);

        Foo foo2 = db.find(Foo.class, response.getId(), response.getRev());
        assertNotNull(foo2);
    }

    @Test
    public void findJsonObject() {
        Response response = db.save(new Foo());
        JsonObject jsonObject = db.find(JsonObject.class, response.getId());
        assertNotNull(jsonObject);
    }

    @Test
    public void findAny() {
        String uri = account.getBaseUri() + "/_all_dbs";
        JsonArray jsonArray = db.findAny(JsonArray.class, uri);
        assertNotNull(jsonArray);
    }

    @Test
    public void findInputstream() throws IOException {
        Response response = db.save(new Foo());
        InputStream inputStream = db.find(response.getId());
        assertTrue(inputStream.read() != -1);
        inputStream.close();
    }

    @Test
    public void findWithParams() {
        Response response = db.save(new Foo());
        Foo foo = db.find(Foo.class, response.getId(), new Params().revsInfo());
        assertNotNull(foo);
    }

    @Test
    public void findWithInvalidId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                db.find(Foo.class, "");
            }
        });
    }

    @Test
    public void findWithUnknownId_throwsNoDocumentException() {
        assertThrows(NoDocumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                db.find(Foo.class, generateUUID());
            }
        });
    }

    @Test
    public void contains() {
        Response response = db.save(new Foo());
        boolean found = db.contains(response.getId());
        assertTrue(found);

        found = db.contains(generateUUID());
        assertFalse(found);
    }

    // Save

    @Test
    public void savePOJO() {
        db.save(new Foo());
    }

    @Test
    public void saveMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("_id", generateUUID());
        map.put("field1", "value1");
        db.save(map);
    }

    @Test
    public void saveJsonObject() {
        JsonObject json = new JsonObject();
        json.addProperty("_id", generateUUID());
        json.add("json-array", new JsonArray());
        db.save(json);
    }

    @Test
    public void saveWithIdContainSlash() {
        String idWithSlash = "a/b/" + generateUUID();
        Response response = db.save(new Foo(idWithSlash));
        assertEquals(idWithSlash, response.getId());
    }

    @Test
    public void saveObjectPost() {
        // database generated id will be assigned
        Response response = db.post(new Foo());
        assertNotNull(response.getId());
    }

    @Test
    public void saveInvalidObject_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                db.save(null);
            }
        });
    }

    @Test
    public void saveNewDocWithRevision_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                Bar bar = new Bar();
                bar.setRevision("unkown");
                db.save(bar);
            }
        });
    }

    @Test
    public void saveDocWithDuplicateId_throwsDocumentConflictException() {
        assertThrows(DocumentConflictException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                String id = generateUUID();
                db.save(new Foo(id));
                db.save(new Foo(id));
            }
        });
    }

    // Update

    @Test
    public void update() {
        Response response = db.save(new Foo());
        Foo foo = db.find(Foo.class, response.getId());
        db.update(foo);
    }

    @Test
    public void updateWithIdContainSlash() {
        String idWithSlash = "a/" + generateUUID();
        Response response = db.save(new Bar(idWithSlash));

        Bar bar = db.find(Bar.class, response.getId());
        Response responseUpdate = db.update(bar);
        assertEquals(idWithSlash, responseUpdate.getId());
    }

    @Test
    public void updateWithoutIdAndRev_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                db.update(new Foo());
            }
        });
    }

    @Test
    public void updateWithInvalidRev_throwsDocumentConflictException() {
        assertThrows(DocumentConflictException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                Response response = db.save(new Foo());
                Foo foo = db.find(Foo.class, response.getId());
                db.update(foo);
                db.update(foo);
            }
        });
    }

    // Delete

    @Test
    public void deleteObject() {
        Response response = db.save(new Foo());
        Foo foo = db.find(Foo.class, response.getId());
        db.remove(foo);
    }

    @Test
    public void deleteByIdAndRevValues() {
        Response response = db.save(new Foo());
        db.remove(response.getId(), response.getRev());
    }

    @Test
    public void deleteByIdContainSlash() {
        String idWithSlash = "a/" + generateUUID();
        Response response = db.save(new Bar(idWithSlash));

        Response responseRemove = db.remove(response.getId(), response.getRev());
        assertEquals(idWithSlash, responseRemove.getId());
    }

    @Test
    public void testCedilla() {
        Foo f = new Foo();
        f.setTitle("François");
        db.save(f);
    }

    // Helper

    private static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
