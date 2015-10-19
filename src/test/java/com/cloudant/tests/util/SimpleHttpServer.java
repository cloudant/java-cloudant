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

package com.cloudant.tests.util;

import org.apache.commons.io.IOUtils;
import org.junit.rules.ExternalResource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;

/**
 * Basis for a simple http server for testing. Allows only a single connection at a time and
 * processes a single request at a time.
 */
public class SimpleHttpServer extends ExternalResource implements Runnable {

    protected static final Logger log = Logger.getLogger(SimpleHttpServer.class.getName());

    protected String PROTOCOL = "http";

    private final Thread serverThread = new Thread(this);
    private final ServerSocketFactory ssf;

    protected final Semaphore semaphore = new Semaphore(0, true);
    protected final AtomicBoolean finished = new AtomicBoolean(false);
    private ServerSocket serverSocket = null;
    private List<String> lines = Collections.emptyList();

    public SimpleHttpServer() {
        this(null);
    }

    public SimpleHttpServer(ServerSocketFactory ssf) {
        this.ssf = (ssf == null) ? ServerSocketFactory.getDefault() : ssf;
    }

    @Override
    protected void before() throws Throwable {
        start();
    }

    @Override
    protected void after() {
        stop();
    }

    /**
     * Called automatically by before when used as a JUnit ExternalResource.
     * Start is exposed here for manual use.
     */
    public void start() throws Exception {
        serverThread.start();
    }

    /**
     * Called automatically by after when used as a JUnit ExternalResource.
     * Stop is exposed here for manual use.
     */
    public void stop() {
        finished.set(true);
        IOUtils.closeQuietly(serverSocket);
        try {
            //wait for the server thread to finish
            serverThread.join();
        } catch (InterruptedException e) {
            log.log(Level.SEVERE, SimpleHttpServer.class.getName() + " interrupted", e);
        }
    }

    @Override
    public void run() {
        try {
            try {
                //create a dynamic socket, and allow only 1 connection
                serverSocket = ssf.createServerSocket(0, 1);
            } catch (SocketException e) {
                log.severe("Unable to open server socket");
                finished.set(true);
            }
            // Listening to the port
            while (!finished.get() && !Thread.currentThread().isInterrupted()) {
                Socket socket = null;
                InputStream is = null;
                OutputStream os = null;
                try {
                    log.fine("Server waiting for connections");
                    //release a permit as we are about to accept connections
                    semaphore.release();
                    //block for connections
                    socket = serverSocket.accept();

                    log.fine("Server accepted connection");

                    is = socket.getInputStream();
                    os = socket.getOutputStream();

                    //do something with the request and then go round the loop again
                    serverAction(is, os);
                } finally {
                    IOUtils.closeQuietly(is);
                    IOUtils.closeQuietly(os);
                    IOUtils.closeQuietly(socket);
                }
            }
            log.fine("Server stopping");
        } catch (Exception e) {
            log.log(Level.SEVERE, SimpleHttpServer.class.getName() + " exception", e);
        } finally {
            semaphore.release();
        }
    }

    /**
     * Wait up to 2 minutes for the server, longer than that and we give up on the test
     *
     * @throws InterruptedException
     */
    public void await() throws InterruptedException {
        semaphore.tryAcquire(2, TimeUnit.MINUTES);
    }

    /**
     * @return the url the server is running on, including dynamic port
     */
    public String getUrl() {
        return PROTOCOL + "://127.0.0.1:" + serverSocket.getLocalPort();
    }

    public SocketAddress getSocketAddress() {
        return serverSocket.getLocalSocketAddress();
    }

    /**
     * Subclasses can override to do something with the input and output streams. The default is
     * to consume the input stream and write a 200.
     *
     * @param is
     * @param os
     */
    protected void serverAction(InputStream is, OutputStream os) throws Exception {
        readInputLines(is);
        writeOK(os);
    }

    /**
     * Write a simple OK response
     *
     * @param os
     * @throws IOException
     */
    protected void writeOK(OutputStream os) throws IOException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os));
        //note the HTTP spec says the status line must be followed by a CRLF
        //and an empty line must separate headers from optional body
        //so basically we need 2 CRLFs
        w.write("HTTP/1.0 200 OK\r\n\r\n");
        w.flush();
    }

    /**
     * Read the input stream lines into a list retrievable via
     * {@link #getLastInputRequestLines}
     *
     * @param is
     * @throws IOException
     */
    protected void readInputLines(InputStream is) throws IOException {
        lines = new ArrayList<String>();
        BufferedReader r = null;
        r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String line;
        while ((line = r.readLine()) != null && !line.isEmpty()) {
            lines.add(line);
        }
    }

    public List<String> getLastInputRequestLines() {
        return lines;
    }
}
