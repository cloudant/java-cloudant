/*
 * Copyright © 2016 IBM Corp. All rights reserved.
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
package com.cloudant.http.internal.interceptors;

import com.cloudant.http.HttpConnectionInterceptorContext;
import com.cloudant.http.HttpConnectionRequestInterceptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

/**
 * Created by Rhys Short on 14/11/2016.
 * @api_private
 */
public class UserAgentInterceptor implements HttpConnectionRequestInterceptor {


    private final String userAgent;

    /**
     * Creates an UserAgentInterceptor
     * @param loader The class loader from which to load the file.
     * @param filepath The path to the resource.
     */
    public UserAgentInterceptor(ClassLoader loader, String filepath){
        String prefix = UserAgentInterceptor.loadUA(loader, filepath);
        String runtimeVersion = System.getProperty("java.version", "Unknown");
        if (runtimeVersion.equals("0")){
            // running on android.
            // since it may fail at runtime to get the SDK version, default to "Unknown" before
            // attempting to get the SDK version.
            runtimeVersion = "Unknown";
            try {
                Class c = Class.forName("android.os.Build$VERSION");
                runtimeVersion = String.valueOf(c.getField("SDK_INT").getInt(null));
            } catch (IllegalAccessException e) {
                // do nothing, just swallow.
            } catch (NoSuchFieldException e) {
                // do nothing, just swallow.
            } catch (ClassNotFoundException e) {
                // do nothing, just swallow.
            }
        }

        this.userAgent =  String.format("%s/%s/%s/%s/%s",
                prefix,
                runtimeVersion,
                System.getProperty("java.vendor"),
                System.getProperty("os.name"),
                System.getProperty("os.arch"));
    }

    /**
     * Loads the properties file using the classloader provided. Creating a string from the properties
     * "user.agent.name" and "user.agent.version".
     * @param loader The class loader to use to load the resource.
     * @param filename The name of the file to load.
     * @return A string that represents the first part of the UA string eg java-cloudant/2.6.1
     */
    private static String loadUA(ClassLoader loader, String filename){
        String ua = "cloudant-http";
        String version = "unknown";
        final InputStream propStream = loader.getResourceAsStream(filename);
        final Properties properties = new Properties();
        try {
            if (propStream != null) {
                try {
                    properties.load(propStream);
                } finally {
                    propStream.close();
                }
            }
            ua = properties.getProperty("user.agent.name", ua);
            version = properties.getProperty("user.agent.version", version);
        } catch (IOException e) {
            // Swallow exception and use default values.
        }

        return String.format(Locale.ENGLISH, "%s/%s", ua,version);
    }

    @Override
    public HttpConnectionInterceptorContext interceptRequest(HttpConnectionInterceptorContext
                                                                          context) {
        context.connection.getConnection().setRequestProperty("User-Agent", userAgent);
        return context;
    }

    public String getUserAgent(){
        return this.userAgent;
    }
}
