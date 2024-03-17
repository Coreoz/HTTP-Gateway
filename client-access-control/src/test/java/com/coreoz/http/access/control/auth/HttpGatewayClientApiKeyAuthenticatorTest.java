package com.coreoz.http.access.control.auth;

import com.google.common.net.HttpHeaders;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import play.mvc.Http;

import java.util.List;

public class HttpGatewayClientApiKeyAuthenticatorTest {
    @Test
    public void authenticate__when_absent_auth_header_returns_null() {
        HttpGatewayClientApiKeyAuthenticator authenticator = makeAuthenticator();
        String clientId = authenticator.authenticate(new Http.RequestBuilder().build());
        Assertions.assertThat(clientId).isNull();
    }

    @Test
    public void authenticate__when_malformed_auth_header_returns_null() {
        HttpGatewayClientApiKeyAuthenticator authenticator = makeAuthenticator();
        String clientId = authenticator.authenticate(new Http.RequestBuilder().header(HttpHeaders.AUTHORIZATION, "appkp").build());
        Assertions.assertThat(clientId).isNull();
    }

    @Test
    public void authenticate__when_no_client_matches_auth_header_returns_null() {
        HttpGatewayClientApiKeyAuthenticator authenticator = makeAuthenticator();
        String clientId = authenticator.authenticate(new Http.RequestBuilder().header(HttpHeaders.AUTHORIZATION, HttpGatewayAuthApiKey.AUTHORIZATION_BEARER_PREFIX + "aerezr").build());
        Assertions.assertThat(clientId).isNull();
    }

    @Test
    public void authenticate__when_a_client_matches_auth_header_returns_client_id() {
        HttpGatewayClientApiKeyAuthenticator authenticator = makeAuthenticator();
        String clientId = authenticator.authenticate(new Http.RequestBuilder().header(HttpHeaders.AUTHORIZATION, HttpGatewayAuthApiKey.AUTHORIZATION_BEARER_PREFIX + "test-key-2").build());
        Assertions.assertThat(clientId).isEqualTo("id-2");
    }

    private HttpGatewayClientApiKeyAuthenticator makeAuthenticator() {
        return new HttpGatewayClientApiKeyAuthenticator(List.of(
            new HttpGatewayClientAuth<>("id-1", new HttpGatewayAuthApiKey("test-key-1")),
            new HttpGatewayClientAuth<>("id-2", new HttpGatewayAuthApiKey("test-key-2"))
        ));
    }
}
