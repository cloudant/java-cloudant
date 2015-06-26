package com.cloudant.tests.util;

import com.cloudant.client.api.CloudantClient;

import org.apache.commons.logging.Log;

import java.io.InputStream;
import java.util.Properties;

public class Utils {
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
}
