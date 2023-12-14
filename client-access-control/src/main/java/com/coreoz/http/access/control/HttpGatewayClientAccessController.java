package com.coreoz.http.access.control;

import com.coreoz.http.access.control.auth.HttpGatewayClientAuthenticator;
import com.coreoz.http.access.control.routes.HttpGatewayClientRouteAccessController;

public interface HttpGatewayClientAccessController extends HttpGatewayClientAuthenticator, HttpGatewayClientRouteAccessController {
}
