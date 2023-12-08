package com.coreoz.http;

import com.coreoz.http.mock.LocalHttpClient;
import com.coreoz.http.mock.SparkMockServer;
import com.coreoz.http.remoteservices.HttpGatewayRemoteService;
import com.google.common.net.HttpHeaders;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Test;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.function.Function;

public class SampleCustomRoutingTest {
    private static HttpGateway httpGateway = SampleCustomRouting.startsGateway();

    static {
        SparkMockServer.initialize();
    }

    @AfterClass
    public static void stopGateway() {
        httpGateway.stop();
    }

    @Test
    public void indexServicesByCustomer__verify_that_customer_a_is_indexed_and_contain_specific_service_a() {
        Config config = ConfigFactory.load("custom-routing.conf");
        Map<String, SampleCustomRouting.RoutingPerCustomer> servicesIndexByCustomer = SampleCustomRouting.indexServicesByCustomer(config);
        Assertions.assertThat(servicesIndexByCustomer).containsKey("customer-a");
        Assertions
            .assertThat(servicesIndexByCustomer.get("customer-a").getServicesIndex().getServices().stream().map(HttpGatewayRemoteService::getServiceId))
            .contains("custom-routes-for-customer-a");
    }

    @Test
    public void verify_that_base_service_is_reachable_for_customer_a() {
        HttpResponse<String> httpResponse = makeHttpRequest("/hello", requestBuilder -> requestBuilder
            .header(HttpHeaders.AUTHORIZATION, "Bearer auth-a")
            .GET());
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("World");
    }

    @Test
    public void verify_that_common_route_for_customer_a_is_redirected_to_service_a() {
        HttpResponse<String> httpResponse = makeHttpRequest("/other-route/aaa", requestBuilder -> requestBuilder
            .header(HttpHeaders.AUTHORIZATION, "Bearer auth-a")
            .GET());
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("Customer A: aaa");
    }

    @Test
    public void verify_that_custom_route_for_customer_a_is_reachable_for_customer_a() {
        HttpResponse<String> httpResponse = makeHttpRequest("/custom-route", requestBuilder -> requestBuilder
            .header(HttpHeaders.AUTHORIZATION, "Bearer auth-a")
            .GET());
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("Customer A custom route");
    }

    @Test
    public void verify_that_custom_route_for_customer_a_is_note_reachable_for_customer_b() {
        HttpResponse<String> httpResponse = makeHttpRequest("/custom-route", requestBuilder -> requestBuilder
            .header(HttpHeaders.AUTHORIZATION, "Bearer auth-b")
            .GET());
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.NOT_FOUND.code());
    }

    @Test
    public void verify_that_common_route_for_customer_b_is_redirected_to_service_b() {
        HttpResponse<String> httpResponse = makeHttpRequest("/other-route/abc", requestBuilder -> requestBuilder
            .header(HttpHeaders.AUTHORIZATION, "Bearer auth-b")
            .GET());
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("Customer B: abc");
    }

    @SneakyThrows
    private static HttpResponse<String> makeHttpRequest(String path) {
        return makeHttpRequest(path, HttpRequest.Builder::GET);
    }

    @SneakyThrows
    public static HttpResponse<String> makeHttpRequest(String path, Function<HttpRequest.Builder, HttpRequest.Builder> with) {
        return LocalHttpClient.makeHttpRequest(SampleCustomRouting.HTTP_GATEWAY_PORT, path, with);
    }
}
