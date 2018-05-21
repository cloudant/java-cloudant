/*
 * Copyright © 2016, 2018 IBM Corp. All rights reserved.
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

package com.cloudant.tests;

import static com.cloudant.tests.util.MockWebServerResources.IAM_TOKEN;
import static com.cloudant.tests.util.MockWebServerResources.OK_IAM_COOKIE;
import static com.cloudant.tests.util.MockWebServerResources.IAM_API_KEY;
import static com.cloudant.tests.util.MockWebServerResources.iamTokenEndpoint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.tests.extensions.MockWebServerExtension;
import com.cloudant.tests.util.IamSystemPropertyMock;
import com.cloudant.tests.util.MockWebServerResources;
import com.google.gson.GsonBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CloudFoundryServiceTest {

    public static IamSystemPropertyMock iamSystemPropertyMock;

    private String mockServerHostPort;

    @RegisterExtension
    public MockWebServerExtension mockWebServerExt = new MockWebServerExtension();

    @RegisterExtension
    public MockWebServerExtension mockIamServerExt = new MockWebServerExtension();

    public MockWebServer server;
    public MockWebServer mockIamServer;

    /**
     * Before running this test class setup the property mock.
     */
    @BeforeAll
    public static void setupIamSystemPropertyMock() {
        iamSystemPropertyMock = new IamSystemPropertyMock();
    }

    @BeforeEach
    public void beforeEach() {
        server = mockWebServerExt.get();
        server.useHttps(MockWebServerResources.getSSLSocketFactory(), false);
        mockServerHostPort = String.format("%s:%s/", server.getHostName(), server.getPort());
        //setup mock IAM server
        mockIamServer = mockIamServerExt.get();
        iamSystemPropertyMock.setMockIamTokenEndpointUrl(mockIamServer.url(iamTokenEndpoint)
                .toString());
    }

    private static class VCAPGenerator {

        private Map<String, Object> vcap;
        private List<HashMap<String, Object>> cloudantServices;

        VCAPGenerator() {
            this("cloudantNoSQLDB");
        }

        VCAPGenerator(String serviceName) {
            vcap = new HashMap<String, Object>();
            cloudantServices = new ArrayList<HashMap<String, Object>>();
            vcap.put(serviceName, cloudantServices);
        }
        
        private void addService(String name, String username, String password,
                                String host, String apikey) {
            HashMap<String, Object> cloudantService = new HashMap<String, Object>();
            cloudantServices.add(cloudantService);

            HashMap<String, Object> cloudantCredentials = new HashMap<String, Object>();
            cloudantService.put("credentials", cloudantCredentials);

            if (name != null) {
                cloudantService.put("name", name);
            }
            if (apikey != null) {
                cloudantCredentials.put("apikey", apikey);
            }
            if (host != null) {
                cloudantCredentials.put("host", host);
            }
            if (username != null) {
                cloudantCredentials.put("username", username);
            }
            if (password != null) {
                cloudantCredentials.put("password", password);
            }
        }

        public void createNewService(String name, String host, String apikey) {
            addService(name, null, null, host, apikey);
        }

        public void createNewServiceWithEmptyIAM(String name, String host) {
            addService(name, null, null, host, null);
        }

        public void createNewLegacyService(String name, String host, String username, String password) {
            addService(name, username, password, host,null);
        }

        public void createNewLegacyServiceWithEmptyCredentials(String name, String host) {
            addService(name, null, null, host, null);
        }

        public String toJson() {
            return new GsonBuilder().create().toJson(vcap);
        }
    }

    @Test
    public void vcapValidServiceNameSpecified() throws Exception {
        //server.enqueue(MockWebServerResources.OK_COOKIE);
        String serviceName = "serviceFoo";
        VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator(serviceName);
        vcap.createNewLegacyService("test_bluemix_service_1",
                String.format("%s:%s/", server.getHostName(), server.getPort()),
                "user", "password");
        CloudantClient client = ClientBuilder
                .bluemix(vcap.toJson(), serviceName, "test_bluemix_service_1")
                .disableSSLAuthentication()
                .build();
        this.testMockInfoRequest(client, true);
        // One request to _session then one to get server info
        assertEquals(2, server.getRequestCount(), "There should be two requests");
    }

    @Test
    public void vcapMissingServiceNameSpecified() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewLegacyService("test_bluemix_service_1",
                        CloudantClientHelper.COUCH_HOST,
                        CloudantClientHelper.COUCH_USERNAME, CloudantClientHelper.COUCH_PASSWORD);
                ClientBuilder.bluemix(vcap.toJson(), "missingService", "test_bluemix_service_1")
                        .build();
            }
        });
    }

    @Test
    public void vcapNullServiceNameSpecified() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewLegacyService("test_bluemix_service_1",
                        CloudantClientHelper.COUCH_HOST,
                        CloudantClientHelper.COUCH_USERNAME, CloudantClientHelper.COUCH_PASSWORD);
                ClientBuilder.bluemix(vcap.toJson(), null, "test_bluemix_service_1").build();
            }
        });
    }

    @Test
    public void vcapSingleServiceWithName() throws Exception{
        VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
        vcap.createNewLegacyService("test_bluemix_service_1",
                mockServerHostPort,
                "user", "password");
        CloudantClient client = ClientBuilder
                .bluemix(vcap.toJson(), "test_bluemix_service_1")
                .disableSSLAuthentication()
                .build();
        this.testMockInfoRequest(client, true);
        // One request to _session then one to get server info
        assertEquals(2, server.getRequestCount(), "There should be two requests");
    }

    @Test
    public void vcapSingleServiceNoNameSpecified() throws Exception {
        VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
        vcap.createNewLegacyService("test_bluemix_service_1",
                mockServerHostPort,
                "user", "password");
        CloudantClient client = ClientBuilder
                .bluemix(vcap.toJson())
                .disableSSLAuthentication()
                .build();
        this.testMockInfoRequest(client, true);
        // One request to _session then one to get server info
        assertEquals(2, server.getRequestCount(), "There should be two requests");
    }

    @Test
    public void vcapSingleServiceWithIAM() throws Exception{
        VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
        vcap.createNewService("test_bluemix_service_1",
                mockServerHostPort,
                IAM_API_KEY);
        CloudantClient client = ClientBuilder
                .bluemix(vcap.toJson(), "test_bluemix_service_1")
                .disableSSLAuthentication()
                .build();
        this.testMockInfoRequest(client, false);
        // One request to _iam_session then one to get server info
        assertEquals(2, server.getRequestCount(), "There should be two requests");
    }

    @Test
    public void vcapSingleServiceMissingNamedService() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewLegacyService("test_bluemix_service_1", "foo1.bar", "admin1",
                        "pass1");
                ClientBuilder.bluemix(vcap.toJson(), "test_bluemix_service_2");
            }
        });
    }

    @Test
    public void vcapSingleServiceEmptyCredentials() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewLegacyServiceWithEmptyCredentials("test_bluemix_service_1",
                        CloudantClientHelper.COUCH_HOST);
                ClientBuilder.bluemix(vcap.toJson(), "test_bluemix_service_1");
            }
        });
    }

    @Test
    public void vcapSingleServiceEmptyCredentialsAndHost() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewLegacyServiceWithEmptyCredentials("test_bluemix_service_1",null);
                ClientBuilder.bluemix(vcap.toJson(), "test_bluemix_service_1");
            }
        });
    }

    @Test
    public void vcapSingleServiceEmptyIAM() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewServiceWithEmptyIAM("test_bluemix_service_1",
                        CloudantClientHelper.COUCH_HOST);
                ClientBuilder.bluemix(vcap.toJson(), "test_bluemix_service_1");
            }
        });
    }

    @Test
    public void vcapSingleServiceEmptyIAMAndHost() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewServiceWithEmptyIAM("test_bluemix_service_1", null);
                ClientBuilder.bluemix(vcap.toJson(), "test_bluemix_service_1");
            }
        });
    }

    @Test
    public void vcapMultiService() throws Exception {
        VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
        vcap.createNewLegacyService("test_bluemix_service_1", "foo1.bar", "admin1", "pass1");
        vcap.createNewLegacyService("test_bluemix_service_2", "foo2.bar", "admin2", "pass2");
        vcap.createNewLegacyService("test_bluemix_service_3",
                mockServerHostPort,
                "user", "password");
        vcap.createNewService("test_bluemix_service_4", "admin4", "api1234key");
        vcap.createNewService("test_bluemix_service_5", mockServerHostPort,
                "api1234key");
        CloudantClient client = ClientBuilder
                .bluemix(vcap.toJson(), "test_bluemix_service_3")
                .disableSSLAuthentication()
                .build();
        this.testMockInfoRequest(client, true);
        // One request to _session then one to get server info
        assertEquals(2, server.getRequestCount(), "There should be two requests");
    }

    @Test
    public void vcapMultiServiceUsingIAM() throws Exception{
        VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
        vcap.createNewLegacyService("test_bluemix_service_1", "foo1.bar", "admin1", "pass1");
        vcap.createNewLegacyService("test_bluemix_service_2", "foo2.bar", "admin2", "pass2");
        vcap.createNewLegacyService("test_bluemix_service_3",
                mockServerHostPort,
                "user", "password");
        vcap.createNewService("test_bluemix_service_4", "admin4", "api1234key");
        vcap.createNewService("test_bluemix_service_5", mockServerHostPort,
                IAM_API_KEY);
        CloudantClient client = ClientBuilder
                .bluemix(vcap.toJson(), "test_bluemix_service_5")
                .disableSSLAuthentication()
                .build();
        this.testMockInfoRequest(client, false);
        // One request to _iam_session then one to get server info
        assertEquals(2, server.getRequestCount(), "There should be two requests");
    }

    @Test
    public void vcapMultiServiceNoNameSpecified() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewLegacyService("test_bluemix_service_1", "foo1.bar", "admin1",
                        "pass1");
                vcap.createNewLegacyService("test_bluemix_service_2", "foo2.bar", "admin2",
                        "pass2");
                vcap.createNewLegacyService("test_bluemix_service_3", "foo3.bar", "admin3",
                        "pass3");
                vcap.createNewService("test_bluemix_service_4", "admin4", "api1234key");
                ClientBuilder.bluemix(vcap.toJson());
            }
        });
    }

    @Test
    public void vcapMultiServiceMissingNamedService() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewLegacyService("test_bluemix_service_1", "foo1.bar", "admin1",
                        "pass1");
                vcap.createNewLegacyService("test_bluemix_service_2", "foo2.bar", "admin2",
                        "pass2");
                vcap.createNewLegacyService("test_bluemix_service_3", "foo3.bar", "admin3",
                        "pass3");
                vcap.createNewService("test_bluemix_service_4", "admin4", "api1234key");
                ClientBuilder.bluemix(vcap.toJson(), "test_bluemix_service_5");
            }
        });
    }

    @Test
    public void vcapMultiServiceEmptyCredentials() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewLegacyService("test_bluemix_service_1", "foo1.bar", "admin1",
                        "pass1");
                vcap.createNewLegacyService("test_bluemix_service_2", "foo2.bar", "admin2",
                        "pass2");
                vcap.createNewLegacyServiceWithEmptyCredentials("test_bluemix_service_3",
                        CloudantClientHelper.COUCH_HOST);
                ClientBuilder.bluemix(vcap.toJson(), "test_bluemix_service_3");
            }
        });
    }

    @Test
    public void vcapMultiServiceEmptyIAM() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewService("test_bluemix_service_1", "admin1", "apikey1");
                vcap.createNewService("test_bluemix_service_2", "admin2", "apikey2");
                vcap.createNewServiceWithEmptyIAM("test_bluemix_service_3",
                        CloudantClientHelper.COUCH_HOST);
                ClientBuilder.bluemix(vcap.toJson(), "test_bluemix_service_3");
            }
        });
    }

    @Test
    public void vcapNoServicesPresent() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                ClientBuilder.bluemix(new CloudFoundryServiceTest.VCAPGenerator().toJson());
            }
        });
    }

    @Test
    public void vcapInvalidJSON() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                ClientBuilder.bluemix("{\"cloudantNoSQLDB\":[]"); // invalid JSON
            }
        });
    }

    @Test
    public void vcapNotPresent() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                ClientBuilder.bluemix(null);
            }
        });
    }

    private void testMockInfoRequest(CloudantClient client, boolean isCookieAuth) throws Exception {
        if (isCookieAuth) {
            // Mock a 200 OK for the _session request
            server.enqueue(MockWebServerResources.OK_COOKIE);
        } else {
            // Request sequence
            // _iam_session request to get Cookie
            // GET request -> 200 with a Set-Cookie
            server.enqueue(OK_IAM_COOKIE);

            mockIamServer.enqueue(new MockResponse().setResponseCode(200).setBody(IAM_TOKEN));
        }
        // 200 with the server info
        MockResponse serverInfoResponse = new MockResponse().setResponseCode(200)
                .setBody("{\"couchdb\":\"Welcome\"," +
                        "\"version\":\"2.1.1\",\"vendor\":{\"name\":\"IBM Cloudant\"," +
                        "\"version\":\"6919\",\"variant\":\"paas\"},\"features\":[\"geo\",\"scheduler\"," +
                        "\"iam\"]}");
        server.enqueue(serverInfoResponse);
        assertEquals("2.1.1", client.serverVersion(), "The server version should be returned correctly");

        if (isCookieAuth) {
            // _session request
            RecordedRequest cookieRequest = server.takeRequest();
            assertEquals("POST", cookieRequest.getMethod(), "The request method should be POST");
            assertEquals("/_session", cookieRequest.getPath(), "The request should be to the _session path");

            // server info request
            RecordedRequest request = server.takeRequest();
            assertEquals("GET", request.getMethod(), "The request method should be GET");
            assertEquals("/", request.getPath(), "The request should be for /");
        } else {
            MockWebServerResources.assertMockIamRequests(server, mockIamServer);
        }
    }
}
