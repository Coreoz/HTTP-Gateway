package com.coreoz.http.upstreamauth;

import com.coreoz.http.access.control.auth.HttpGatewayAuthBasic;
import com.google.common.net.HttpHeaders;
import org.asynchttpclient.RequestBuilder;
import play.mvc.Http;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HttpGatewayRemoteServiceBasicAuthenticator implements HttpGatewayUpstreamAuthenticator {
    private final String authorizationBasic;

    // TODO unit test this
    public HttpGatewayRemoteServiceBasicAuthenticator(HttpGatewayAuthBasic authBasic) {
        this.authorizationBasic = HttpGatewayAuthBasic.AUTHORIZATION_BASIC_PREFIX
            + Base64.getEncoder().encodeToString(
            (authBasic.getObjectId() + ':' + authBasic.getPassword())
                .getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public void accept(Http.Request request, RequestBuilder requestBuilder) {
        requestBuilder.setHeader(
            HttpHeaders.AUTHORIZATION,
            authorizationBasic
        );
    }
}
