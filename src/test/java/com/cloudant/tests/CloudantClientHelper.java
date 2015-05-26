package com.cloudant.tests;

import com.cloudant.client.api.CloudantClient;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Rhys Short on 21/05/15.
 */
public abstract class CloudantClientHelper {

    protected static final String COUCH_USERNAME ;
    protected static final String COUCH_PASSWORD ;
    protected static final String COUCH_HOST ;
    protected static final String COUCH_PORT;
    protected static final String HTTP_PROTOCOL ;
    protected static final URI SERVER_URI;
    protected static final boolean IGNORE_CLOUDANT_SPECIFIC;


    static {


        String URI = System.getProperty("test.couch.uri");
        IGNORE_CLOUDANT_SPECIFIC = !Boolean.parseBoolean(System.getProperty(
                "test.cloudant.specific",
                Boolean.toString(Boolean.FALSE)));

        if(URI == null) {

            COUCH_USERNAME = System.getProperty("test.couch.username");
            COUCH_PASSWORD = System.getProperty("test.couch.password");
            COUCH_HOST = System.getProperty("test.couch.host", "localhost");
            COUCH_PORT = System.getProperty("test.couch.port", "5984");
            HTTP_PROTOCOL = System.getProperty("test.couch.http", "http"); //should either be http or https
            try {

                if(COUCH_USERNAME == null || COUCH_PASSWORD == null){
                    SERVER_URI = new URI(String.format("%s://%s:%s",
                            HTTP_PROTOCOL,
                            COUCH_HOST,
                            COUCH_PORT));
                } else {
                    SERVER_URI = new URI(String.format("%s://%s:%s@%s:%s",
                            HTTP_PROTOCOL,
                            COUCH_USERNAME,
                            COUCH_PASSWORD,
                            COUCH_HOST,
                            COUCH_PORT));
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } else {

            try {
                SERVER_URI = new URI(URI);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }


            if(SERVER_URI.getRawUserInfo() == null){
                COUCH_USERNAME = null;
                COUCH_PASSWORD = null;
            } else {
                COUCH_USERNAME = SERVER_URI.getRawUserInfo().substring(0, SERVER_URI.getRawUserInfo()
                        .indexOf(":"));
                COUCH_PASSWORD = SERVER_URI.getRawUserInfo().substring(SERVER_URI.getRawUserInfo()
                        .indexOf(":"));
            }
            COUCH_HOST = SERVER_URI.getHost();
            COUCH_PORT = Integer.toString(SERVER_URI.getPort());
            HTTP_PROTOCOL = SERVER_URI.getScheme();


        }
    }

    public static CloudantClient getClient(){
       return new CloudantClient(SERVER_URI.toString(),
               COUCH_USERNAME,COUCH_PASSWORD
        );
    }


}
