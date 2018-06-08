/*
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

package com.cloudant.client.api.model;

/**
 * <p>
 * Base class for serialisation and deserialisation of Cloudant Documents.
 * </p>
 *
 * <p>
 * This class and its subclasses serve the following purposes:
 * </p>
 * <ul>
 * <li>Response model for {@code _all_docs} and views</li>
 * <li>Design documents</li>
 * <li>Replicator documents</li>
 * </ul>
 *
 * <p>
 * This class or its subclasses can also be used for APIs like
 * {@link com.cloudant.client.api.Database#find(Class, String)} where deserialisation is performed,
 * but it is useful to retain fields such as {@code _id}, {@code _rev}, {@code _deleted} for
 * subsequent operations.
 * </p>
 *
 * <p>
 * Because this class only represents a "skeleton" document, users should consider sub-classing
 * {@link com.cloudant.client.api.model.Document} in order to map other fields, for example:
 * </p>
 *
 * <pre>
 * {@code
 * import com.cloudant.client.api.model.Document;
 *
 * public class EmployeeDocument extends Document {
 *   String firstName;
 *   String lastName;
 *   // etc
 * }
 * }
 * </pre>
 *
 * <p>
 * Note that the normal
 * <a href="https://github.com/google/gson/blob/master/UserGuide.md" target="_blank">Gson</a>
 * serialisation rules apply and that annotations like {@code SerializedName} can be used.
 * </p>
 *
 * <p>
 * <b>Important:</b>Top-level field names that begin with the underscore character (_) are
 * <a href="https://console.bluemix.net/docs/services/Cloudant/api/document.html#documents" target="_blank">
 * reserved</a> in Cloudant and must not be used. If fine-grained control of reserved properties
 * such as {@code _id}, {@code _rev}, {@code _deleted} is required, then they should be mapped to
 * fields in a custom class which does <b>not</b> subclass {@link com.cloudant.client.api.model.Document}.
 * </p>
 *
 * @author Ganesh K Choudhary
 * @since 0.0.1
 */
public class Document extends com.cloudant.client.org.lightcouch.Document {

    public void addAttachment(String name, Attachment attachment) {
        super.addAttachment(name, attachment.getAttachement());
    }

}
