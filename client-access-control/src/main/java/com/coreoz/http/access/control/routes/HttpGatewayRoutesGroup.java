package com.coreoz.http.access.control.routes;

import lombok.Value;

import java.util.List;

@Value
public class HttpGatewayRoutesGroup {
    String routesGroupId;
    List<String> routeIds;
}
