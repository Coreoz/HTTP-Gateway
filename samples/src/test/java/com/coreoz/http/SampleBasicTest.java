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

public class SampleBasicTest {
    private static HttpGateway httpGateway = SampleBasic.startsGateway();

    static {
        SparkMockServer.initialize();
    }

    @AfterClass
    public static void stopGateway() {
        httpGateway.stop();
    }

    // verify rewrite rule avec client 2
    @Test
    public void verify_that_client_zoo_can_access_rewrite_route() {
        HttpResponse<String> httpResponse = makeHttpRequest("/custom-pets/hhh/custom-route", requestBuilder -> requestBuilder
            .header(HttpHeaders.AUTHORIZATION, "Bearer auth-zoo")
            .GET());
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("Fetch pet hhh");
    }

    @Test
    public void verify_that_client_zoo_has_access_to_permitted_route_add_pets() {
        HttpResponse<String> httpResponse = makeHttpRequest("/pets", requestBuilder -> requestBuilder
            .header(HttpHeaders.AUTHORIZATION, "Bearer auth-zoo")
            .POST(HttpRequest.BodyPublishers.ofString("sample-body"))
        );
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("Addede sample-body");
    }

    @Test
    public void verify_that_client_zoo_does_not_have_access_to_unpermitted_route_fetch_pet_friends() {
        HttpResponse<String> httpResponse = makeHttpRequest("/pets/aaa/friends", requestBuilder -> requestBuilder
            .header(HttpHeaders.AUTHORIZATION, "Bearer auth-zoo")
            .GET());
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.UNAUTHORIZED.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo(
            "{\"message\":\"Access denied to route GET /pets/aaa/friends for clientId app-zoo\"}"
        );
    }

    @Test
    public void verify_that_client_other_does_not_have_access_to_unpermitted_route_fetch_pets() {
        HttpResponse<String> httpResponse = makeHttpRequest("/pets", requestBuilder -> requestBuilder
            .header(HttpHeaders.AUTHORIZATION, "Bearer other-app-key")
            .GET());
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.UNAUTHORIZED.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo(
            "{\"message\":\"Access denied to route GET /pets for clientId other-app\"}"
        );
    }

    @Test
    public void verify_that_client_other_can_access_service_that_does_not_have_authentication() {
        HttpResponse<String> httpResponse = makeHttpRequest("/route-sample", requestBuilder -> requestBuilder
            .header(HttpHeaders.AUTHORIZATION, "Bearer other-app-key")
            .GET());
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("Another route");
    }

    @SneakyThrows
    public static HttpResponse<String> makeHttpRequest(String path, Function<HttpRequest.Builder, HttpRequest.Builder> with) {
        return LocalHttpClient.makeHttpRequest(SampleCustomRouting.HTTP_GATEWAY_PORT, path, with);
    }
}
