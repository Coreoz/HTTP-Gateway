package com.coreoz.http.router;

import com.coreoz.http.HttpGateway;
import com.coreoz.http.conf.HttpGatewayConfiguration;
import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import com.coreoz.http.mock.LocalHttpClient;
import com.coreoz.http.mock.SparkMockServer;
import com.coreoz.http.play.HttpGatewayDownstreamResponses;
import com.coreoz.http.routes.HttpRoutes;
import com.coreoz.http.routes.parsing.DestinationRoute;
import com.coreoz.http.upstream.HttpGatewayUpstreamClient;
import com.coreoz.http.upstream.HttpGatewayUpstreamRequest;
import com.coreoz.http.upstream.HttpGatewayUpstreamResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HttpGatewayTest {
    static int HTTP_GATEWAY_PORT = 9876;

    @Test
    public void integration_test__verify_that_server_and_gateway_client_is_working() {
        SparkMockServer.initialize();

        HttpGatewayUpstreamClient httpGatewayUpstreamClient = new HttpGatewayUpstreamClient();
        HttpGatewayRouter httpRouter = new HttpGatewayRouter(List.of(
            new HttpGatewayRoute("endpoint1", "GET", "/endpoint1", HttpRoutes.parsePathAsSegments("/hello")),
            new HttpGatewayRoute("endpoint2", "GET", "/endpoint2/{id}", HttpRoutes.parsePathAsSegments("/echo/{id}"))
        ));
        HttpGateway httpGateway = HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            routerDsl -> routerDsl.addRoutes(HttpGatewayRouterConfiguration.asyncRouting(downstreamRequest -> {
                DestinationRoute destinationRoute = httpRouter
                    .searchRoute(downstreamRequest.method(), downstreamRequest.path())
                    .map(httpRouter::computeDestinationRoute)
                    .orElse(null);
                if (destinationRoute == null) {
                    return HttpGatewayDownstreamResponses.buildError(HttpResponseStatus.NOT_FOUND, "No route exists for " + downstreamRequest.method() + " " + downstreamRequest.path());
                }

                HttpGatewayUpstreamRequest remoteRequest = httpGatewayUpstreamClient
                    .prepareRequest(downstreamRequest)
                    .withUrl("http://localhost:" + SparkMockServer.SPARK_HTTP_PORT + destinationRoute.destinationPath())
                    .copyBasicHeaders()
                    .copyQueryParams();
                CompletableFuture<HttpGatewayUpstreamResponse> upstreamFutureResponse = httpGatewayUpstreamClient.executeUpstreamRequest(remoteRequest);
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

        HttpResponse<String> httpResponse = makeHttpRequest("/endpoint1");
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("World");

        httpResponse = makeHttpRequest("/endpoint2/custom-param-value?param1=value1&param2=value2");
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("""
            custom-param-value\

            param1=value1&param2=value2\

            accept-header=custom_accept\

            authorization=null"""
        );

        httpGateway.stop();
    }

    private static HttpResponse<String> makeHttpRequest(String path) {
        return LocalHttpClient.makeHttpGetRequest(HTTP_GATEWAY_PORT, path);
    }
}
