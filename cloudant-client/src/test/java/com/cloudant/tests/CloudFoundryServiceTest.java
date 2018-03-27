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

import com.cloudant.client.api.ClientBuilder;
import com.google.gson.GsonBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CloudFoundryServiceTest {

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
    public void vcapValidServiceNameSpecified() {
        String serviceName = "serviceFoo";
        VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator(serviceName);
        vcap.createNewService("test_bluemix_service_1",
                CloudantClientHelper.SERVER_URI_WITH_USER_INFO,
                CloudantClientHelper.COUCH_USERNAME, CloudantClientHelper.COUCH_PASSWORD);
        ClientBuilder.bluemix(vcap.toJson(), serviceName, "test_bluemix_service_1").build().serverVersion();
    }

    @Test
    public void vcapMissingServiceNameSpecified() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewService("test_bluemix_service_1",
                        CloudantClientHelper.SERVER_URI_WITH_USER_INFO,
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
                vcap.createNewService("test_bluemix_service_1",
                        CloudantClientHelper.SERVER_URI_WITH_USER_INFO,
                        CloudantClientHelper.COUCH_USERNAME, CloudantClientHelper.COUCH_PASSWORD);
                ClientBuilder.bluemix(vcap.toJson(), null, "test_bluemix_service_1").build();
            }
        });
    }

    @Test
    public void vcapSingleServiceWithName() {
        VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
        vcap.createNewService("test_bluemix_service_1",
                CloudantClientHelper.SERVER_URI_WITH_USER_INFO,
                CloudantClientHelper.COUCH_USERNAME, CloudantClientHelper.COUCH_PASSWORD);
        ClientBuilder.bluemix(vcap.toJson(), "test_bluemix_service_1").build().serverVersion();
    }

    @Test
    public void vcapSingleServiceNoNameSpecified() {
        VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
        vcap.createNewService("test_bluemix_service_1",
                CloudantClientHelper.SERVER_URI_WITH_USER_INFO,
                CloudantClientHelper.COUCH_USERNAME, CloudantClientHelper.COUCH_PASSWORD);
        ClientBuilder.bluemix(vcap.toJson()).build().serverVersion();
    }

    @Test
    public void vcapSingleServiceMissingNamedService() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewService("test_bluemix_service_1", "http://foo1.bar", "admin1",
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
                vcap.createNewServiceWithEmptyCredentials("test_bluemix_service_1");
                ClientBuilder.bluemix(vcap.toJson(), "test_bluemix_service_1");
            }
        });
    }

    @Test
    public void vcapMultiService() {
        VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
        vcap.createNewService("test_bluemix_service_1","http://foo1.bar", "admin1", "pass1");
        vcap.createNewService("test_bluemix_service_2","http://foo2.bar", "admin2", "pass2");
        vcap.createNewService("test_bluemix_service_3",
                CloudantClientHelper.SERVER_URI_WITH_USER_INFO,
                CloudantClientHelper.COUCH_USERNAME, CloudantClientHelper.COUCH_PASSWORD);
        ClientBuilder.bluemix(vcap.toJson(), "test_bluemix_service_3").build().serverVersion();
    }

    @Test
    public void vcapMultiServiceNoNameSpecified() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewService("test_bluemix_service_1", "http://foo1.bar", "admin1",
                        "pass1");
                vcap.createNewService("test_bluemix_service_2", "http://foo2.bar", "admin2",
                        "pass2");
                vcap.createNewService("test_bluemix_service_3", "http://foo3.bar", "admin3",
                        "pass3");
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
                vcap.createNewService("test_bluemix_service_1", "http://foo1.bar", "admin1",
                        "pass1");
                vcap.createNewService("test_bluemix_service_2", "http://foo2.bar", "admin2",
                        "pass2");
                vcap.createNewService("test_bluemix_service_3", "http://foo3.bar", "admin3",
                        "pass3");
                ClientBuilder.bluemix(vcap.toJson(), "test_bluemix_service_4");
            }
        });
    }

    @Test
    public void vcapMultiServiceEmptyCredentials() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                VCAPGenerator vcap = new CloudFoundryServiceTest.VCAPGenerator();
                vcap.createNewService("test_bluemix_service_1", "http://foo1.bar", "admin1",
                        "pass1");
                vcap.createNewService("test_bluemix_service_2", "http://foo2.bar", "admin2",
                        "pass2");
                vcap.createNewServiceWithEmptyCredentials("test_bluemix_service_3");
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
}
