package com.coreoz.http.upstreamauth;

import com.coreoz.http.access.control.auth.HttpGatewayAuthApiKey;
import com.google.common.net.HttpHeaders;
import org.assertj.core.api.Assertions;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.junit.Test;

public class HttpGatewayRemoteServiceKeyAuthenticatorTest {

    @Test
    public void customize__verify_that_the_authentication_header_key_is_added_to_the_request() {
        HttpGatewayRemoteServiceKeyAuthenticator keyAuthenticator = makeAuthenticator();
        RequestBuilder remoteServiceRequestBuilder = new RequestBuilder();
        keyAuthenticator.customize(null, remoteServiceRequestBuilder);
        Request remoteServiceRequest = remoteServiceRequestBuilder.build();
        Assertions.assertThat(remoteServiceRequest.getHeaders().get(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer auth-key");
    }

    public HttpGatewayRemoteServiceKeyAuthenticator makeAuthenticator() {
        return new HttpGatewayRemoteServiceKeyAuthenticator(
            new HttpGatewayAuthApiKey("auth-key")
        );
    }
}
