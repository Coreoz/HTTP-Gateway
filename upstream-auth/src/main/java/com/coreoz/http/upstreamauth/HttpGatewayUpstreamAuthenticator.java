package com.coreoz.http.upstreamauth;

import org.asynchttpclient.RequestBuilder;
import play.mvc.Http;

@FunctionalInterface
public interface HttpGatewayUpstreamAuthenticator {
    void addUpstreamAuthentication(Http.Request downstreamRequest, RequestBuilder upstreamRequest);
}
