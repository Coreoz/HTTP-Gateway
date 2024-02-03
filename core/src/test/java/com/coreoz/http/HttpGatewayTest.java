package com.coreoz.http;

import com.coreoz.http.conf.HttpGatewayConfiguration;
import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import com.coreoz.http.mock.LocalHttpClient;
import com.coreoz.http.mock.SparkMockServer;
import com.coreoz.http.play.HttpGatewayDownstreamResponses;
import com.coreoz.http.upstream.HttpGatewayUpstreamClient;
import com.coreoz.http.upstream.HttpGatewayUpstreamRequest;
import com.coreoz.http.upstream.HttpGatewayUpstreamResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import play.mvc.Results;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class HttpGatewayTest {
    static int HTTP_GATEWAY_PORT = 9876;

    static {
        SparkMockServer.initialize();
    }

    @Test
    public void integration_test__verify_that_server_starts_and_is_working() throws IOException, InterruptedException {
        HttpGateway httpGateway = HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            routerDsl -> routerDsl.addRoutes(route -> route.POST("/test").routingTo((request) -> Results.ok("Hello world !")))
        ));

        HttpResponse<String> httpResponse = makeHttpRequest("/test");

        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("Hello world !");

        httpGateway.stop();
    }

    @Test
    public void integration_test__verify_that_server_and_async_router_is_working() throws IOException, InterruptedException {
        HttpGateway httpGateway = HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            routerDsl -> routerDsl.addRoutes(HttpGatewayRouterConfiguration.asyncRouting(request -> CompletableFuture.completedFuture(Results.ok("Hello world !"))))
        ));

        HttpResponse<String> httpResponse = makeHttpRequest("/endpoint-not-used-here");

        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("Hello world !");

        httpGateway.stop();
    }

    @Test
    public void integration_test__verify_that_upstream_request_is_correctly_proxied() throws IOException, InterruptedException {
        HttpGatewayUpstreamClient httpGatewayUpstreamClient = new HttpGatewayUpstreamClient();
        HttpGateway httpGateway = HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            routerDsl -> routerDsl.addRoutes(HttpGatewayRouterConfiguration.asyncRouting(request -> {
                HttpGatewayUpstreamRequest upstreamRequest = httpGatewayUpstreamClient
                    .prepareRequest(request)
                    .withUrl("http://localhost:" + SparkMockServer.SPARK_HTTP_PORT + request.path());
                CompletableFuture<HttpGatewayUpstreamResponse> upstreamFutureResponse = httpGatewayUpstreamClient.executeUpstreamRequest(upstreamRequest);
                return upstreamFutureResponse.thenApply(upstreamResponse -> {
                    if (upstreamResponse.getStatusCode() > HttpResponseStatus.INTERNAL_SERVER_ERROR.code()) {
                        // Do not forward the response body if the upstream server returns an internal error
                        // => this enables to avoid forwarding sensitive information
                        upstreamResponse.setPublisher(null);
                    }

                    return HttpGatewayDownstreamResponses.buildResult(upstreamResponse);
                });
            }))
        ));

        HttpResponse<String> httpResponse = makeHttpRequest("/long-body");

        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("This is a loooooooooooooooooong body");

        httpGateway.stop();
    }

    private static HttpResponse<String> makeHttpRequest(String path) throws IOException, InterruptedException {
        return LocalHttpClient.makeHttpRequest(HTTP_GATEWAY_PORT, path, request -> request.POST(HttpRequest.BodyPublishers.ofString("body post content")));
    }
}
