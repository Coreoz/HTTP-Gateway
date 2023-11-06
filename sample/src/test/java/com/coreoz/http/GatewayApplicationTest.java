package com.coreoz.http;

import com.coreoz.http.mock.LocalHttpClient;
import com.coreoz.http.mock.SparkMockServer;
import com.google.common.net.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Function;

public class GatewayApplicationTest {
    private static HttpGateway httpGateway = GatewayApplication.startsGateway();

    static {
        SparkMockServer.initialize();
    }

    @Test
    public void verify_that_unknown_client_returns_401_response() {
        HttpResponse<String> httpResponse = makeHttpRequest("/lots-of-pets");
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.UNAUTHORIZED.code());
    }

    @Test
    public void verify_that_correct_client_auth_request_is_correctly_routed_to_authenticated_service() {
        HttpResponse<String> httpResponse = makeHttpRequest("/lots-of-pets", requestBuilder -> requestBuilder
            .header(HttpHeaders.AUTHORIZATION, "Bearer auth-zoo")
            .GET());
        Assertions.assertThat(httpResponse.body()).isEqualTo("Lots of pets :)");
    }

    @SneakyThrows
    private static HttpResponse<String> makeHttpRequest(String path) {
        return makeHttpRequest(path, HttpRequest.Builder::GET);
    }

    @SneakyThrows
    public static HttpResponse<String> makeHttpRequest(String path, Function<HttpRequest.Builder, HttpRequest.Builder> with) {
        return LocalHttpClient.makeHttpRequest(GatewayApplication.HTTP_GATEWAY_PORT, path, with);
    }
}
