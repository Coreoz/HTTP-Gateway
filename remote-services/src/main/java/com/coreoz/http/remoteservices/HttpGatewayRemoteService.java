package com.coreoz.http.remoteservices;

import lombok.Value;

import java.util.List;

@Value
public class HttpGatewayRemoteService {
    String serviceId;
    String baseUrl;
    List<HttpGatewayRemoteServiceRoute> routes;
}
