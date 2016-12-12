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

package com.cloudant.tests;

import com.cloudant.client.api.ClientBuilder;
import com.google.gson.GsonBuilder;

import org.junit.Test;

import mockit.Mock;
import mockit.MockUp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CloudFoundryServiceTest {

    private static class VCAPGenerator {

        private Map<String, Object> vcap;
        private List<HashMap<String, Object>> cloudantServices;

        VCAPGenerator() {
            vcap = new HashMap<String, Object>();
            cloudantServices = new ArrayList<HashMap<String, Object>>();
            vcap.put("cloudantNoSQLDB", cloudantServices);
        }

        private void addService(String name, String url, String username, String password) {
            HashMap<String, Object> cloudantService = new HashMap<String, Object>();
            cloudantServices.add(cloudantService);

            HashMap<String, Object> cloudantCredentials = new HashMap<String, Object>();
            cloudantService.put("credentials", cloudantCredentials);

            if (name != null) {
                cloudantService.put("name", name);
            }
            if (url != null) {
                cloudantCredentials.put("url", url);
            }
            if (username != null) {
                cloudantCredentials.put("username", url);
            }
            if (password != null) {
                cloudantCredentials.put("password", url);
            }
        }

        public void createNewService(String name, String url, String username, String password) {
            addService(name, url, username, password);
        }

        public void createNewServiceWithEmptyCredentials(String name) {
            addService(name, null, null, null);
        }

        public void createNewServiceWithoutName(String url, String username, String password) {
            addService(null, url, username, password);
        }

        public String toJson() {
            return new GsonBuilder().create().toJson(vcap);
        }
    }

    @Test
    public void vcapSingleServiceWithName() {
        new MockUp<System>() {
            @Mock
            public String getenv(final String string) {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewService("test_bluemix_service_1",
                        CloudantClientHelper.SERVER_URI_WITH_USER_INFO,
                        CloudantClientHelper.COUCH_USERNAME, CloudantClientHelper.COUCH_PASSWORD);
                return vcap.toJson();
            }
        };
        ClientBuilder.bluemix("test_bluemix_service_1").build().serverVersion();
    }

    @Test
    public void vcapSingleServiceNoNameSpecified() {
        new MockUp<System>() {
            @Mock
            public String getenv(final String string) {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewService("test_bluemix_service_1",
                        CloudantClientHelper.SERVER_URI_WITH_USER_INFO,
                        CloudantClientHelper.COUCH_USERNAME, CloudantClientHelper.COUCH_PASSWORD);
                return vcap.toJson();
            }
        };
        ClientBuilder.bluemix().build().serverVersion();
    }

    @Test(expected = IllegalArgumentException.class)
    public void vcapSingleServiceMissingNamedService() {
        new MockUp<System>() {
            @Mock
            public String getenv(final String string) {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewService("test_bluemix_service_1","http://foo1.bar", "admin1", "pass1");
                return vcap.toJson();
            }
        };
        ClientBuilder.bluemix("test_bluemix_service_2");
    }

    @Test(expected = IllegalStateException.class)
    public void vcapSingleServiceEmptyCredentials() {
        new MockUp<System>() {
            @Mock
            public String getenv(final String string) {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewServiceWithEmptyCredentials("test_bluemix_service_1");
                return vcap.toJson();
            }
        };
        ClientBuilder.bluemix("test_bluemix_service_1");
    }

    @Test
    public void vcapMultiService() {
        new MockUp<System>() {
            @Mock
            public String getenv(final String string) {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewService("test_bluemix_service_1","http://foo1.bar", "admin1", "pass1");
                vcap.createNewService("test_bluemix_service_2","http://foo2.bar", "admin2", "pass2");
                vcap.createNewService("test_bluemix_service_3",
                        CloudantClientHelper.SERVER_URI_WITH_USER_INFO,
                        CloudantClientHelper.COUCH_USERNAME, CloudantClientHelper.COUCH_PASSWORD);
                return vcap.toJson();
            }
        };
        ClientBuilder.bluemix("test_bluemix_service_3").build().serverVersion();
    }

    @Test(expected = IllegalArgumentException.class)
    public void vcapMultiServiceNoNameSpecified() {
        new MockUp<System>() {
            @Mock
            public String getenv(final String string) {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewService("test_bluemix_service_1","http://foo1.bar", "admin1", "pass1");
                vcap.createNewService("test_bluemix_service_2","http://foo2.bar", "admin2", "pass2");
                vcap.createNewService("test_bluemix_service_3","http://foo3.bar", "admin3", "pass3");
                return vcap.toJson();
            }
        };
        ClientBuilder.bluemix();
    }

    @Test(expected = IllegalArgumentException.class)
    public void vcapMultiServiceMissingNamedService() {
        new MockUp<System>() {
            @Mock
            public String getenv(final String string) {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewService("test_bluemix_service_1","http://foo1.bar", "admin1", "pass1");
                vcap.createNewService("test_bluemix_service_2","http://foo2.bar", "admin2", "pass2");
                vcap.createNewService("test_bluemix_service_3","http://foo3.bar", "admin3", "pass3");
                return vcap.toJson();
            }
        };
        ClientBuilder.bluemix("test_bluemix_service_4");
    }

    @Test(expected = IllegalStateException.class)
    public void vcapMultiServiceEmptyCredentials() {
        new MockUp<System>() {
            @Mock
            public String getenv(final String string) {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewService("test_bluemix_service_1","http://foo1.bar", "admin1", "pass1");
                vcap.createNewService("test_bluemix_service_2","http://foo2.bar", "admin2", "pass2");
                vcap.createNewServiceWithEmptyCredentials("test_bluemix_service_3");
                return vcap.toJson();
            }
        };
        ClientBuilder.bluemix("test_bluemix_service_3");
    }

    @Test(expected = IllegalStateException.class)
    public void vcapNoServicesPresent() {
        new MockUp<System>() {
            @Mock
            public String getenv(final String string) {
                return new CloudFoundryServiceTest.VCAPGenerator().toJson();
            }
        };
        ClientBuilder.bluemix();
    }

    @Test(expected = IllegalStateException.class)
    public void vcapInvalidJSON() {
        new MockUp<System>() {
            @Mock
            public String getenv(final String string) {
                return "{\"cloudantNoSQLDB\":[]"; // invalid JSON
            }
        };
        ClientBuilder.bluemix();
    }

    @Test(expected = IllegalStateException.class)
    public void vcapNotPresent() {
        new MockUp<System>() {
            @Mock
            public String getenv(final String string) {
                return null;
            }
        };
        ClientBuilder.bluemix();
    }
}
