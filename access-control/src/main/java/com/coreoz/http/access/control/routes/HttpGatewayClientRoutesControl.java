package com.coreoz.http.access.control.routes;

import lombok.Value;

import java.util.List;

@Value
public class HttpGatewayClientRoutesControl {
    String clientId;
    List<String> restrictedRoutes;
    List<String> restrictedRoutesGroups;
    List<String> restrictedServices;
}
