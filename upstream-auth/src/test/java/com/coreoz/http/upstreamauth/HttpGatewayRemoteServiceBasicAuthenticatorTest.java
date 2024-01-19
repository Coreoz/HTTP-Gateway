package com.coreoz.http.upstreamauth;

import com.coreoz.http.access.control.auth.HttpGatewayAuthBasic;
import com.google.common.net.HttpHeaders;
import org.assertj.core.api.Assertions;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.junit.Test;

public class HttpGatewayRemoteServiceBasicAuthenticatorTest {

    @Test
    public void getAuthorizationBasic__verify_that_the_correct_header_value_is_generated() {
        HttpGatewayRemoteServiceBasicAuthenticator basicAuthenticator = makeAuthenticator();
        Assertions.assertThat(basicAuthenticator.getAuthorizationBasic()).isEqualTo("Basic dXNlci10ZXN0OnVzZXItcGFzc3dvcmQ=");
    }

    @Test
    public void customize__verify_that_the_basic_header_is_added_to_the_request() {
        HttpGatewayRemoteServiceBasicAuthenticator basicAuthenticator = makeAuthenticator();
        RequestBuilder remoteServiceRequestBuilder = new RequestBuilder();
        basicAuthenticator.customize(null, remoteServiceRequestBuilder);
        Request remoteServiceRequest = remoteServiceRequestBuilder.build();
        Assertions.assertThat(remoteServiceRequest.getHeaders().get(HttpHeaders.AUTHORIZATION)).isEqualTo("Basic dXNlci10ZXN0OnVzZXItcGFzc3dvcmQ=");
    }

    public HttpGatewayRemoteServiceBasicAuthenticator makeAuthenticator() {
        return new HttpGatewayRemoteServiceBasicAuthenticator(
            new HttpGatewayAuthBasic("service-id-test", "user-test", "user-password")
        );
    }
}
