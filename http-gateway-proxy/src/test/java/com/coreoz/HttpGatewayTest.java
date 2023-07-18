package com.coreoz;

import com.coreoz.client.HttpGatewayClient;
import com.coreoz.client.HttpGatewayRemoteRequest;
import com.coreoz.client.HttpGatewayRemoteResponse;
import com.coreoz.conf.HttpGatewayConfiguration;
import com.coreoz.conf.HttpGatewayRouterConfiguration;
import com.coreoz.mock.SparkMockServer;
import com.coreoz.play.HttpGatewayResponses;
import com.coreoz.router.HttpGatewayRouter;
import com.coreoz.router.data.HttpEndpoint;
import com.coreoz.router.data.TargetRoute;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import play.mvc.Results;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.coreoz.play.HttpGatewayResponses.buildError;

public class HttpGatewayTest {
    static int HTTP_GATEWAY_PORT = 9876;

    @Test
    public void integration_test__verify_that_server_starts_and_is_working() throws IOException, InterruptedException {
        HttpGateway httpGateway = HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            (router) -> router.GET("/test").routingTo((request) -> Results.ok("Hello world !")).build()
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
            HttpGatewayRouterConfiguration.asyncRouting(request -> CompletableFuture.completedFuture(Results.ok("Hello world !")))
        ));

        HttpResponse<String> httpResponse = makeHttpRequest("/test");

        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("Hello world !");

        httpGateway.stop();
    }

    @Test
    public void integration_test__verify_that_server_and_gateway_client_is_working() throws IOException, InterruptedException {
        SparkMockServer.initialize();

        HttpGatewayClient httpGatewayClient = new HttpGatewayClient();
        // TODO create endpoint list from config
        HttpGatewayRouter<String> httpRouter = new HttpGatewayRouter<>(List.of(
            HttpEndpoint.of("endpoint1", "GET", "/endpoint1", "/hello", "http://localhost:" + SparkMockServer.SPARK_HTTP_PORT),
            HttpEndpoint.of("endpoint2", "GET", "/endpoint2/{id}", "/echo/{id}", "http://localhost:" + SparkMockServer.SPARK_HTTP_PORT)
        ));
        HttpGateway httpGateway = HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            HttpGatewayRouterConfiguration.asyncRouting(request -> {
                TargetRoute<String> targetRoute = httpRouter
                    .searchRoute(request.method(), request.path())
                    .map(httpRouter::computeTargetRoute)
                    .orElse(null);
                if (targetRoute == null) {
                    return buildError(HttpResponseStatus.NOT_FOUND, "No route exists for " + request.method() + " " + request.path());
                }

                // TODO ajouter du publisher peeker via la méthode preparePeekerReques
                HttpGatewayRemoteRequest remoteRequest = httpGatewayClient
                    .prepareRequest(request)
                    .withUrl(targetRoute.getTargetUrl())
                    .copyBasicHeaders()
                    .copyQueryParams();
                CompletableFuture<HttpGatewayRemoteResponse> remoteResponse = httpGatewayClient.executeRemoteRequest(remoteRequest);
                return remoteResponse.thenApply(upstreamResponse -> {
                    if (upstreamResponse.getStatusCode() > HttpResponseStatus.INTERNAL_SERVER_ERROR.code()) {
                        // Do not forward the response body if the upstream server returns an internal error
                        // => this enables to avoid forwarding sensitive information
                        upstreamResponse.setPublisher(null);
                    }

                    return HttpGatewayResponses.buildResult(upstreamResponse);
                });
            })
        ));

        HttpResponse<String> httpResponse = makeHttpRequest("/endpoint1");
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("World");

        httpResponse = makeHttpRequest("/endpoint2/custom-param-value");
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("custom-param-value");

        httpGateway.stop();
    }

    private static HttpResponse<String> makeHttpRequest(String path) throws IOException, InterruptedException {
        return HttpClient.newHttpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + HTTP_GATEWAY_PORT + path))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }
}
