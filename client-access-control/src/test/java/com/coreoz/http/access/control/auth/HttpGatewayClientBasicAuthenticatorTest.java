package com.coreoz.http.access.control.auth;

import com.google.common.net.HttpHeaders;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import play.mvc.Http;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class HttpGatewayClientBasicAuthenticatorTest {
    @Test
    public void authenticate__when_absent_auth_header_returns_null() {
        HttpGatewayClientBasicAuthenticator authenticator = makeAuthenticator();
        String clientId = authenticator.authenticate(new Http.RequestBuilder().build());
        Assertions.assertThat(clientId).isNull();
    }

    @Test
    public void authenticate__when_malformed_auth_header_returns_null() {
        HttpGatewayClientBasicAuthenticator authenticator = makeAuthenticator();
        String clientId = authenticator.authenticate(new Http.RequestBuilder().header(HttpHeaders.AUTHORIZATION, "appkp").build());
        Assertions.assertThat(clientId).isNull();
    }

    @Test
    public void authenticate__when_malformed_basic_header_returns_null() {
        HttpGatewayClientBasicAuthenticator authenticator = makeAuthenticator();
        String clientId = authenticator.authenticate(new Http.RequestBuilder().header(HttpHeaders.AUTHORIZATION, HttpGatewayAuthBasic.AUTHORIZATION_BASIC_PREFIX + "aerezr").build());
        Assertions.assertThat(clientId).isNull();
    }

    @Test
    public void authenticate__when_no_client_matches_basic_header_returns_null() {
        HttpGatewayClientBasicAuthenticator authenticator = makeAuthenticator();
        String clientId = authenticator.authenticate(new Http.RequestBuilder().header(HttpHeaders.AUTHORIZATION, HttpGatewayAuthBasic.AUTHORIZATION_BASIC_PREFIX + makeBasicAuth("test", "test")).build());
        Assertions.assertThat(clientId).isNull();
    }

    @Test
    public void authenticate__when_a_client_matches_auth_header_returns_client_id() {
        HttpGatewayClientBasicAuthenticator authenticator = makeAuthenticator();
        String clientId = authenticator.authenticate(new Http.RequestBuilder().header(HttpHeaders.AUTHORIZATION, HttpGatewayAuthBasic.AUTHORIZATION_BASIC_PREFIX + makeBasicAuth("user-1", "password-1")).build());
        Assertions.assertThat(clientId).isEqualTo("id-1");
    }

    private String makeBasicAuth(String login, String password) {
        return Base64.getEncoder().encodeToString((login + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private HttpGatewayClientBasicAuthenticator makeAuthenticator() {
        return new HttpGatewayClientBasicAuthenticator(List.of(
            new HttpGatewayAuthBasic("id-1", "user-1", "password-1"),
            new HttpGatewayAuthBasic("id-2", "user-2", "password-2")
        ));
    }
}
