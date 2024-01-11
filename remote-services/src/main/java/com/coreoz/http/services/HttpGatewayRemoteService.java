package com.coreoz.http.services;

import lombok.Value;

import java.util.List;

@Value
public class HttpGatewayRemoteService {
    String serviceId;
    String baseUrl;
    List<HttpGatewayRemoteServiceRoute> routes;
}
