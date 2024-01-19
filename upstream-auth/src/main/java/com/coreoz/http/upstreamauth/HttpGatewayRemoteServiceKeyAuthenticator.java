package com.coreoz.http.upstreamauth;

import com.coreoz.http.access.control.auth.HttpGatewayAuthApiKey;
import com.google.common.net.HttpHeaders;
import org.asynchttpclient.RequestBuilder;
import play.mvc.Http;

public class HttpGatewayRemoteServiceKeyAuthenticator implements HttpGatewayUpstreamAuthenticator {
    private final String authorizationHeaderValue;

    public HttpGatewayRemoteServiceKeyAuthenticator(HttpGatewayAuthApiKey authApiKeyObject) {
        this.authorizationHeaderValue = HttpGatewayAuthApiKey.AUTHORIZATION_BEARER_PREFIX + authApiKeyObject.getAuthKey();
    }

    @Override
    public void customize(Http.Request request, RequestBuilder requestBuilder) {
        requestBuilder.setHeader(
            HttpHeaders.AUTHORIZATION,
            authorizationHeaderValue
        );
    }
}
