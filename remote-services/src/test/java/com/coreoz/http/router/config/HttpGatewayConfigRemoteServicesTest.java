package com.coreoz.http.router.config;

import com.coreoz.http.HttpGateway;
import com.coreoz.http.client.HttpGatewayUpstreamClient;
import com.coreoz.http.client.HttpGatewayUpstreamRequest;
import com.coreoz.http.client.HttpGatewayUpstreamResponse;
import com.coreoz.http.conf.HttpGatewayConfiguration;
import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import com.coreoz.http.mock.SparkMockServer;
import com.coreoz.http.play.HttpGatewayDownstreamResponses;
import com.coreoz.http.remote.services.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.router.HttpGatewayRouter;
import com.coreoz.http.router.data.DestinationRoute;
import com.google.common.net.HttpHeaders;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class HttpGatewayConfigRemoteServicesTest {
    static int HTTP_GATEWAY_PORT = 9876;

    @Test
    public void integration_test__verify_that_server_and_gateway_client_is_working() throws IOException, InterruptedException {
        SparkMockServer.initialize();

        HttpGatewayUpstreamClient httpGatewayUpstreamClient = new HttpGatewayUpstreamClient();
        Config config = ConfigFactory.load();
        HttpGatewayRemoteServicesIndex servicesIndex = HttpGatewayConfigRemoteServices.indexRemoteServices(config);
        HttpGatewayRouter httpRouter = new HttpGatewayRouter(servicesIndex.getRoutes());
        HttpGateway httpGateway = HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            HttpGatewayRouterConfiguration.asyncRouting(downstreamRequest -> {
                DestinationRoute destinationRoute = httpRouter
                    .searchRoute(downstreamRequest.method(), downstreamRequest.path())
                    .map(httpRouter::computeDestinationRoute)
                    .orElse(null);
                if (destinationRoute == null) {
                    return HttpGatewayDownstreamResponses.buildError(HttpResponseStatus.NOT_FOUND, "No route exists for " + downstreamRequest.method() + " " + downstreamRequest.path());
                }

                // TODO ajouter du publisher peeker via la méthode preparePeekerReques
                HttpGatewayUpstreamRequest remoteRequest = httpGatewayUpstreamClient
                    .prepareRequest(downstreamRequest)
                    .withUrl(destinationRoute.getDestinationUrl())
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
            })
        ));

        HttpResponse<String> httpResponse = makeHttpRequest("/endpoint1");
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("World");

        httpResponse = makeHttpRequest("/endpoint2/custom-param-value?param1=value1&param2=value2");
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("custom-param-value" +
            "\nparam1=value1&param2=value2"
            + "\naccept-header=custom_accept"
            + "\nauthorization=null"
        );

        httpGateway.stop();
    }

    private static HttpResponse<String> makeHttpRequest(String path) throws IOException, InterruptedException {
        return HttpClient.newHttpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + HTTP_GATEWAY_PORT + path))
                .GET()
                .header(HttpHeaders.ACCEPT, "custom_accept")
                .header(HttpHeaders.AUTHORIZATION, "custom_auth")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }
}
