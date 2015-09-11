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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;

/**
 * A very simple HTTP Server that runs on the localhost and can handle a single request.
 */
public class SingleRequestHttpServer implements Runnable {

    private ServerSocket serverSocket;
    private boolean finished;
    private static final Logger log = Logger.getLogger(SingleRequestHttpServer.class.getName());
    private static boolean localServerReady;
    private static final Object lock = new Object();

    private final List<String> input = new ArrayList<String>();
    private final int port;

    private SingleRequestHttpServer(int port) {
        this.port = port;
    }

    public void run() {
        try {
            try {
                serverSocket = ServerSocketFactory.getDefault().createServerSocket(port);
            } catch (SocketException e) {
                log.log(Level.SEVERE, "Unable to open server socket", e);
                finished = true;
            }

            // Listening to the port
            log.log(Level.FINE, "Server waiting for connections");
            Socket socket;
            synchronized (lock) {
                localServerReady = true;
                lock.notify();
            }
            socket = serverSocket.accept();
            localServerReady = false;
            log.log(Level.INFO, "Server accepted connection");

            BufferedReader r = null;
            r = new BufferedReader(new InputStreamReader(socket.getInputStream(),
                    "UTF-8"));
            String line;
            while ((line = r.readLine()) != null && !line.isEmpty()) {
                input.add(line);
            }

            // Just send a simple success response.
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(socket
                    .getOutputStream()));
            w.write("HTTP/1.0 200 OK");
            w.flush();
            w.close();
            socket.close();
        } catch (SocketException e) {
            log.log(Level.WARNING, "Socket closed", e);
        } catch (Exception exception) {
            log.log(Level.SEVERE, "Unexpected exception", exception);
            finished = true;
        } finally {
            closeSocket();
            log.info("Server stopped");
        }
    }

    /**
     * Get the request input
     *
     * @return Collection of Strings one per line of the request input
     */
    public Collection<String> getRequestInput() {
        return input;
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

    /**
     * Block until the server is ready to accept connections
     */
    public void waitForServer() {
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
     * Start an HTTPServer instance on a new thread listening on the specified port
     *
     * @param port to listen on
     */
    public static SingleRequestHttpServer startServer(int port) {
        SingleRequestHttpServer server = new SingleRequestHttpServer(port);
        new Thread(server).start();
        return server;
    }
}
