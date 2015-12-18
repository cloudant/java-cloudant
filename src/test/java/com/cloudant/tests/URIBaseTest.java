package com.cloudant.tests;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.internal.URIBase;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.util.CloudantClientResource;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.URI;

@Category(RequiresDB.class)
public class URIBaseTest {

    @ClassRule
    public static CloudantClientResource clientResource = new CloudantClientResource();

    @Test
    public void buildAccountUri_noTrailingPathSeparator() throws Exception {
        CloudantClient client = ClientBuilder.url(clientResource.get().getBaseUri().toURL())
                .build();
        Assert.assertFalse(client.getBaseUri().toString().endsWith("/"));
        URI clientUri = new URIBase(client.getBaseUri()).build();
        Assert.assertFalse(clientUri.toString().endsWith("/"));

        //Check that path is not missing / separators
        clientUri = new URIBase(client.getBaseUri()).path("").path("api").path("couch").build();
        URI expectedAccountUri = new URI(clientResource.get().getBaseUri().toString()
                + "/api/couch");
        Assert.assertEquals(expectedAccountUri, clientUri);
    }
}
