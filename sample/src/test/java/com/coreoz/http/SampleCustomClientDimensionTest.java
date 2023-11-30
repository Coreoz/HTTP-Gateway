package com.coreoz.http;

import com.coreoz.http.mock.LocalHttpClient;
import com.coreoz.http.mock.SparkMockServer;
import com.google.common.net.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Test;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Function;

public class SampleCustomClientDimensionTest {
    private static HttpGateway httpGateway = SampleCustomClientDimension.startsGateway();

    static {
        SparkMockServer.initialize();
    }

    @AfterClass
    public static void stopGateway() {
        httpGateway.stop();
    }

    @Test
    public void verify_that_unknown_client_returns_401_response() {
        HttpResponse<String> httpResponse = makeHttpRequest("/lots-of-pets");
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.UNAUTHORIZED.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("{\"message\":\"Client authentication failed\"}");
    }

    @Test
    public void verify_that_invalid_custom_header_returns_error() {
        HttpResponse<String> httpResponse = makeHttpRequest("/lots-of-pets", requestBuilder -> requestBuilder
            .header(HttpHeaders.AUTHORIZATION, "Bearer auth-zoo")
            .header(SampleCustomClientDimension.HTTP_HEADER_TENANTS, "unauthorized-tenant")
            .GET());
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.UNAUTHORIZED.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo(
            "{\"message\":\"Access to tenant 'unauthorized-tenant' is not allowed. Allowed tenants are: [site1, site2]\"}"
        );
    }

    @Test
    public void verify_that_custom_header_is_correctly_forwarded_to_service() {
        HttpResponse<String> httpResponse = makeHttpRequest("/lots-of-pets", requestBuilder -> requestBuilder
            .header(HttpHeaders.AUTHORIZATION, "Bearer auth-zoo")
            .header(SampleCustomClientDimension.HTTP_HEADER_TENANTS, "site2")
            .GET());
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.headers().firstValue(SampleCustomClientDimension.HTTP_HEADER_TENANTS))
            .isPresent()
            .hasValue("site2");
    }

    @SneakyThrows
    private static HttpResponse<String> makeHttpRequest(String path) {
        return makeHttpRequest(path, HttpRequest.Builder::GET);
    }

    @SneakyThrows
    public static HttpResponse<String> makeHttpRequest(String path, Function<HttpRequest.Builder, HttpRequest.Builder> with) {
        return LocalHttpClient.makeHttpRequest(SampleCustomClientDimension.HTTP_GATEWAY_PORT, path, with);
    }
}
