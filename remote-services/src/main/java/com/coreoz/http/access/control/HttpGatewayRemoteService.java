package com.coreoz.http.access.control;

import lombok.Value;

import java.util.List;

@Value
public class HttpGatewayRemoteService {
    String serviceId;
    String baseUrl;
    List<HttpGatewayRemoteServiceRoute> routes;
}
