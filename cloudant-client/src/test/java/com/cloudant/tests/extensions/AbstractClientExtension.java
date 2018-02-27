package com.cloudant.tests.extensions;

import com.cloudant.client.api.CloudantClient;

public abstract class AbstractClientExtension {

    public abstract CloudantClient get();

    public abstract String getBaseURIWithUserInfo();

}
