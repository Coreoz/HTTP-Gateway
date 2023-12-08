package com.coreoz.http.access.control.routes;

import lombok.Value;

import java.util.List;

@Value
public class HttpGatewayClientRoutesControl {
    String clientId;
    List<String> allowedRoutes;
    List<String> allowedRoutesGroups;
    List<String> allowedServices;
}
