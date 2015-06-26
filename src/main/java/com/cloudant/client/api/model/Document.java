package com.cloudant.client.api.model;


/**
 * Convenient base class for Cloudant documents, defines the basic
 * <code>id</code>, <code>revision</code> properties, and attachments.
 *
 * @author Ganesh K Choudhary
 * @since 0.0.1
 */
public class Document extends org.lightcouch.Document {

    public void addAttachment(String name, Attachment attachment) {
        // TODO Auto-generated method stub
        super.addAttachment(name, attachment.getAttachement());
    }

}
