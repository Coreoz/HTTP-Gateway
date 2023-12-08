package com.coreoz.http.upstreamauth;

import com.coreoz.http.access.control.auth.HttpGatewayAuthBasic;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HttpHeaders;
import org.asynchttpclient.RequestBuilder;
import play.mvc.Http;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HttpGatewayRemoteServiceBasicAuthenticator implements HttpGatewayUpstreamAuthenticator {
    private final String authorizationBasic;

    public HttpGatewayRemoteServiceBasicAuthenticator(HttpGatewayAuthBasic authBasic) {
        this.authorizationBasic = HttpGatewayAuthBasic.AUTHORIZATION_BASIC_PREFIX
            + Base64.getEncoder().encodeToString(
            (authBasic.getUserId() + ':' + authBasic.getPassword())
                .getBytes(StandardCharsets.UTF_8)
        );
    }

    @VisibleForTesting
    public String getAuthorizationBasic() {
        return authorizationBasic;
    }

    @Override
    public void customize(Http.Request request, RequestBuilder requestBuilder) {
        requestBuilder.setHeader(
            HttpHeaders.AUTHORIZATION,
            authorizationBasic
        );
    }
}
