package com.cloudant.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.model.ConnectOptions;
import com.cloudant.test.main.RequiresCloudantService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.lightcouch.CouchDbException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLHandshakeException;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.security.KeyStore;


public class SslAuthenticationTest {

    private static final Log log = LogFactory.getLog(SslAuthenticationTest.class);
    private static CloudantClient dbClient;
    private static HttpsServer server;

    private static String KEYSTORE_FILE = "src/test/resources/SslAuthenticationTest.keystore";
    private static String KEYSTORE_PASSWORD = "password";
    private static String KEY_PASSWORD = "password";
    private static int LOCAL_SERVER_PORT = 65535;
    private static String LOCAL_SERVER_URL = "https://127.0.0.1:" + LOCAL_SERVER_PORT;

    private static String DUMMY_USERNAME = "username";
    private static String DUMMY_PASSWORD = "password";

    private static boolean localServerReady;
    private static final Object lock = new Object();

    /**
     * A very simple HTTPS Server that runs on the localhost and has a simple certificate that won't
     * work for hostname verification.
     * <p>
     * A suitable simple certificate can be generated using the command:<br />
     *    <code>
     *        keytool -genkey -alias alias -keypass password -keystore SslAuthenticationTest.keystore -storepass password
     *    </code>
     */
    private static class HttpsServer implements Runnable {

        private SSLServerSocket serverSocket;
        private boolean finished;

        public void run() {
            while (!finished) {
                try {
                    KeyStore keystore = KeyStore.getInstance("JKS");
                    keystore.load(new FileInputStream(KEYSTORE_FILE), KEYSTORE_PASSWORD.toCharArray());
                    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    kmf.init(keystore, KEY_PASSWORD.toCharArray());
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(kmf.getKeyManagers(), null, null);
                    SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
                    try {
                        serverSocket = (SSLServerSocket) ssf.createServerSocket(LOCAL_SERVER_PORT);
                    } catch (SocketException e) {
                        log.error("Unable to open server socket");
                        finished = true;
                    }
                    // Listening to the port
                    while (!finished && !Thread.currentThread().isInterrupted()) {
                        log.debug("Server waiting for connections");
                        SSLSocket socket;
                        synchronized (lock) {
                            localServerReady = true;
                            lock.notify();
                        }
                        socket = (SSLSocket) serverSocket.accept();
                        localServerReady = false;
                        log.debug("Server accepted connection");

                        // Just send a simple success response.
                        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        w.write("HTTP/1.0 200 OK");
                        w.flush();
                        w.close();
                        socket.close();
                    }
                } catch (SocketException e) {
                    log.debug("Socket closed");
                } catch (SSLHandshakeException e1) {
                    log.debug("SSL Handshake failed");
                } catch (Exception exception) {
                    exception.printStackTrace();
                    finished = true;
                } finally {
                    closeSocket();
                    log.debug("Server stopped");
                }
            }
        }

        public void stop() {
            log.debug("Stopping server");
            finished = true;
            closeSocket();
        }

        private synchronized void closeSocket() {
            localServerReady = false;
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                serverSocket = null;
            }
        }
    }

    @BeforeClass
    public static void setUpClass() {
        server = new HttpsServer();
        new Thread(server).start();
    }

    @AfterClass
    public static void tearDownClass() {
        server.stop();
    }

    @After
    public void tearDown() {
        if (dbClient != null) {
            dbClient.shutdown();
        }
    }

    /** Wait until the local HTTPS server is ready to accept connections. */
    private void waitForLocalServer() {
        synchronized (lock) {
            try {
                while (!localServerReady) {
                    lock.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check the exception chain is as expected when the SSL host name authentication fails
     * to be sure we got a CouchDbException for the reason we expect.
     * @param e the exception.
     */
    private static void validateClientAuthenticationException(CouchDbException e) {
        assertNotNull("Expected CouchDbException but got null", e);
        Throwable t = e.getCause();
        assertTrue("Expected SSLHandshakeException caused by client certificate check but got " + t.getClass(),
                t instanceof SSLHandshakeException);
    }

    /**
     * Connect to the local simple https server with SSL authentication disabled.
     */
    @Test
    public void localSslAuthenticationDisabled() {
        ConnectOptions connectionOptions = new ConnectOptions();
        connectionOptions.setSSLAuthenticationDisabled(true);

        waitForLocalServer();

        dbClient = new CloudantClient(LOCAL_SERVER_URL,
                DUMMY_USERNAME,
                DUMMY_PASSWORD,
                connectionOptions);

        // Make an arbitrary connection to the DB.
        dbClient.getAllDbs();

        // Test is successful if no exception is thrown, so no explicit check is needed.
    }

    /**
     * Connect to the local simple https server with SSL authentication enabled explicitly.
     * This should throw an exception because the SSL authentication fails.
     */
    @Test
    public void localSslAuthenticationEnabled() {
        ConnectOptions connectionOptions = new ConnectOptions();
        connectionOptions.setSSLAuthenticationDisabled(false);

        waitForLocalServer();

        CouchDbException thrownException = null;
        try {
            dbClient = new CloudantClient(LOCAL_SERVER_URL,
                    DUMMY_USERNAME,
                    DUMMY_PASSWORD,
                    connectionOptions);

            // Make an arbitrary connection to the DB.
            dbClient.getAllDbs();
        } catch (CouchDbException e) {
            thrownException = e;
        }
        validateClientAuthenticationException(thrownException);
    }

    /**
     * Connect to the local simple https server with SSL authentication enabled implicitly.
     * This should throw an exception because the SSL authentication fails.
     */
    @Test
    public void localSslAuthenticationEnabledDefault() {
        waitForLocalServer();

        CouchDbException thrownException = null;
        try {
            dbClient = new CloudantClient(LOCAL_SERVER_URL,
                    DUMMY_USERNAME,
                    DUMMY_PASSWORD);

            // Make an arbitrary connection to the DB.
            dbClient.getAllDbs();
        } catch (CouchDbException e) {
            thrownException = e;
        }
        validateClientAuthenticationException(thrownException);
    }

    /**
     * Connect to the remote Cloudant server with SSL Authentication enabled.
     * This shouldn't throw an exception as the Cloudant server has a valid
     * SSL certificate, so should be authenticated.
     */
    @Test
    @Category(RequiresCloudantService.class)
    public void remoteSslAuthenticationEnabledTest() {
        ConnectOptions connectionOptions = new ConnectOptions();
        connectionOptions.setSSLAuthenticationDisabled(false);

        dbClient = new CloudantClient(CloudantClientHelper.SERVER_URI.toString(),
                CloudantClientHelper.COUCH_USERNAME,
                CloudantClientHelper.COUCH_PASSWORD,
                connectionOptions);

        // Make an arbitrary connection to the DB.
        dbClient.getAllDbs();

        // Test is successful if no exception is thrown, so no explicit check is needed.
    }

    /**
     * Connect to the remote Cloudant server with SSL Authentication disabled.
     */
    @Test
    @Category(RequiresCloudantService.class)
    public void remoteSslAuthenticationDisabledTest() {
        ConnectOptions connectionOptions = new ConnectOptions();
        connectionOptions.setSSLAuthenticationDisabled(true);

        dbClient = new CloudantClient(CloudantClientHelper.SERVER_URI.toString(),
                CloudantClientHelper.COUCH_USERNAME,
                CloudantClientHelper.COUCH_PASSWORD,
                connectionOptions);

        // Make an arbitrary connection to the DB.
        dbClient.getAllDbs();

        // Test is successful if no exception is thrown, so no explicit check is needed.
    }

}

