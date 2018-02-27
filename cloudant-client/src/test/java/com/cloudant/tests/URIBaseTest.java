package com.cloudant.tests;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.internal.URIBase;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.extensions.CloudantClientExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;

@RequiresDB
public class URIBaseTest {

    @RegisterExtension
    public static CloudantClientExtension clientResource = new CloudantClientExtension();

    @Test
    public void buildAccountUri_noTrailingPathSeparator() throws Exception {
        CloudantClient client = ClientBuilder.url(clientResource.get().getBaseUri().toURL())
                .build();
        Assertions.assertFalse(client.getBaseUri().toString().endsWith("/"));
        URI clientUri = new URIBase(client.getBaseUri()).build();
        Assertions.assertFalse(clientUri.toString().endsWith("/"));

        //Check that path is not missing / separators
        clientUri = new URIBase(client.getBaseUri()).path("").path("api").path("couch").build();
        URI expectedAccountUri = new URI(clientResource.get().getBaseUri().toString()
                + "/api/couch");
        Assertions.assertEquals(expectedAccountUri, clientUri);
    }
}
