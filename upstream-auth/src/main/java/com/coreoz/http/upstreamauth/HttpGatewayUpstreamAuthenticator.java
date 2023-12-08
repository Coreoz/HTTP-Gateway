package com.coreoz.http.upstreamauth;

import com.coreoz.http.upstream.HttpGatewayUpstreamRequest;
import org.asynchttpclient.RequestBuilder;
import play.mvc.Http;

@FunctionalInterface
public interface HttpGatewayUpstreamAuthenticator extends HttpGatewayUpstreamRequest.HttpGatewayRequestCustomizer {
    void customize(Http.Request downstreamRequest, RequestBuilder upstreamRequest);
}
