package com.cloudant.tests.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.ReplicatorDocument;
import com.cloudant.client.api.model.Response;

import org.apache.commons.logging.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.utils.HttpClientUtils;

import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

public class Utils {

    private static final long TIMEOUT_MILLISECONDS = 3200;

    public static Properties getProperties(String configFile, Log log) {
        Properties properties = new Properties();
        try {
            InputStream instream = CloudantClient.class.getClassLoader().getResourceAsStream
                    (configFile);
            properties.load(instream);
        } catch (Exception e) {
            String msg = "Could not read configuration file from the classpath: " + configFile;
            log.error(msg);
            throw new IllegalStateException(msg, e);
        }
        return properties;

    }

    public static String getHostName(String account) {
        if (account.startsWith("http://")) {
            return account.substring(7);
        } else if (account.startsWith("https://")) {
            return account.substring(8);
        } else {
            return account + ".cloudant.com";
        }

    }

    public static void removeReplicatorTestDoc(CloudantClient account, String replicatorDocId)
            throws Exception {

        //Grab replicator doc revision using HTTP GET command
        String replicatorDb = "_replicator";
        HttpHead head = new HttpHead(account.getBaseUri() + replicatorDb + "/"
                + replicatorDocId);
        HttpResponse response = account.executeRequest(head);
        String revision = response.getFirstHeader("ETAG").getValue();
        HttpClientUtils.closeQuietly(response);

        Database replicator = account.database(replicatorDb, false);
        Response removeResponse = replicator.remove(replicatorDocId,
                revision.replaceAll("\"", ""));

        assertThat(removeResponse.getError(), is(nullValue()));
    }

    public static ReplicatorDocument waitForReplicatorToStart(CloudantClient account,
                                                              String replicatorDocId)
            throws Exception {
        return waitForReplicatorToReachStatus(account, replicatorDocId, "triggered");
    }

    public static ReplicatorDocument waitForReplicatorToComplete(CloudantClient account,
                                                                 String replicatorDocId)
            throws Exception {
        return waitForReplicatorToReachStatus(account, replicatorDocId, "completed");
    }

    public static ReplicatorDocument waitForReplicatorToReachStatus(CloudantClient account,
                                                                    String replicatorDocId,
                                                                    String status)
            throws Exception {
        ReplicatorDocument replicatorDoc = null;

        boolean finished = false;

        long startTime = System.currentTimeMillis();
        long timeout = startTime + TIMEOUT_MILLISECONDS;
        //initial wait of 100 ms
        long delay = 100;

        while (!finished && System.currentTimeMillis() < timeout) {
            //Sleep before finding replication document
            Thread.sleep(delay);

            replicatorDoc = account.replicator()
                    .replicatorDocId(replicatorDocId)
                    .find();

            //Check if replicator doc is completed or if continuous replication is triggered
            if (replicatorDoc != null && replicatorDoc.getReplicationState() != null
                    && (replicatorDoc.getReplicationState().equalsIgnoreCase(status))) {

                finished = true;
            }
            //double the delay for the next iteration
            delay *= 2;
        }

        return replicatorDoc;
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
