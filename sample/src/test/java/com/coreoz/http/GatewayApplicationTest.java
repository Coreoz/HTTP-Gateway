package com.coreoz.http;

import com.coreoz.http.mock.LocalHttpClient;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.net.http.HttpResponse;

public class GatewayApplicationTest {
    private static HttpGateway httpGateway = GatewayApplication.startsGateway();

    @Test
    public void verify_that_unknown_client_returns_401_response() {
        HttpResponse<String> httpResponse = makeHttpRequest("/lots-of-pets");
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.UNAUTHORIZED.code());
    }

    @SneakyThrows
    private static HttpResponse<String> makeHttpRequest(String path) {
        return LocalHttpClient.makeHttpGetRequest(GatewayApplication.HTTP_GATEWAY_PORT, path);
    }
}
