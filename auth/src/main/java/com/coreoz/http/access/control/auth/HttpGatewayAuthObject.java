package com.coreoz.http.access.control.auth;

public interface HttpGatewayAuthObject {
    /**
     * The object id referenced by this authentication, it can be a clientId or a serviceId
     */
    String getObjectId();
}
