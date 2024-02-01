package com.coreoz.http.access.control.auth;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import play.mvc.Http;

import java.util.List;

public class HttpGatewayClientAuthenticatorTest {
    @Test
    public void merge__verify_that_2_authenticators_are_correctly_merged() {
        HttpGatewayClientAuthenticator authenticatorA = makeAuthenticator("a");
        HttpGatewayClientAuthenticator authenticatorB = makeAuthenticator("b");
        HttpGatewayClientAuthenticator merged = HttpGatewayClientAuthenticator.merge(List.of(authenticatorA, authenticatorB));
        String testA = merged.authenticate(new Http.RequestBuilder().path("/a").build());
        String testB = merged.authenticate(new Http.RequestBuilder().path("/b").build());
        String testC = merged.authenticate(new Http.RequestBuilder().path("/c").build());

        Assertions.assertThat(testA).isEqualTo("a");
        Assertions.assertThat(testB).isEqualTo("b");
        Assertions.assertThat(testC).isNull();
    }

    private HttpGatewayClientAuthenticator makeAuthenticator(String name) {
        return downstreamRequest -> {
            if (("/" + name).equals(downstreamRequest.path())) {
                return name;
            }
            return null;
        };
    }
}
