package com.cloudant.tests;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.org.lightcouch.internal.URIBuilder;
import com.cloudant.http.CookieInterceptor;
import com.cloudant.http.HttpConnection;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.tests.util.CloudantClientResource;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class HttpTest {

    private String data = "{\"hello\":\"world\"}";

    @ClassRule
    public static CloudantClientResource clientResource = new CloudantClientResource();
    private CloudantClient account = clientResource.get();

    //NOTE: This test doesn't work with specified couch servers,
    // the URL will always include the creds specified for the test
    //
    // A couchdb server needs to be set and running with the correct
    // security settings, the database *must* not be public, it *must*
    // be named cookie_test
    //
    @Test
    @Category(RequiresCloudant.class)
    public void testCookieAuthWithoutRetry() throws IOException {

        //Create cookie_test db
        account.createDB("cookie_test");

        CookieInterceptor interceptor = new CookieInterceptor(CloudantClientHelper.COUCH_USERNAME,
                CloudantClientHelper.COUCH_PASSWORD);

        URL cookie_test = URIBuilder.buildUri(account.getBaseUri())
                .path("cookie_test").build().toURL();

        HttpConnection conn = new HttpConnection("POST", cookie_test,
                "application/json");
        conn.responseInterceptors.add(interceptor);
        conn.requestInterceptors.add(interceptor);
        ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes());

        // nothing read from stream
        Assert.assertEquals(bis.available(), data.getBytes().length);

        conn.setRequestBody(bis);
        conn.execute();

        // stream was read to end
        Assert.assertEquals(bis.available(), 0);
        Assert.assertEquals(2, conn.getConnection().getResponseCode() / 100);

        //check the json
        Gson gson = new Gson();
        JsonObject response = gson.fromJson(new InputStreamReader(conn.getConnection()
                .getInputStream()), JsonObject.class);

        Assert.assertTrue(response.has("ok"));
        Assert.assertTrue(response.get("ok").getAsBoolean());
        Assert.assertTrue(response.has("id"));
        Assert.assertTrue(response.has("rev"));

        //Clean up database created
        account.deleteDB("cookie_test");
    }
}
