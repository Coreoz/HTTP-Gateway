package com.coreoz;

import com.coreoz.client.HttpGatewayClient;
import com.coreoz.client.HttpGatewayRemoteRequest;
import com.coreoz.client.HttpGatewayRemoteResponse;
import com.coreoz.conf.HttpGatewayConfiguration;
import com.coreoz.conf.HttpGatewayRouterConfiguration;
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
    static int HTTP_PORT = 9876;

    @Test
    public void integration_test__verify_that_server_starts_and_is_working() throws IOException, InterruptedException {
        HttpGateway httpGateway = HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_PORT,
            (router) -> router.GET("/test").routingTo((request) -> Results.ok("Hello world !")).build()
        ));

        HttpResponse<String> httpResponse = makeHttpRequest();

        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("Hello world !");

        httpGateway.stop();
    }

    @Test
    public void integration_test__verify_that_server_and_async_router_is_working() throws IOException, InterruptedException {
        HttpGateway httpGateway = HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_PORT,
            HttpGatewayRouterConfiguration.asyncRouting(request -> CompletableFuture.completedFuture(Results.ok("Hello world !")))
        ));

        HttpResponse<String> httpResponse = makeHttpRequest();

        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("Hello world !");

        httpGateway.stop();
    }

    @Test
    public void integration_test__verify_that_server_and_gateway_client_is_working() throws IOException, InterruptedException {
        HttpGatewayClient httpGatewayClient = new HttpGatewayClient();
        // TODO create endpoint list from config
        HttpGatewayRouter<String> httpRouter = new HttpGatewayRouter<>(List.of(
            HttpEndpoint.of("endpoint1", "GET", "/endpoint1", "/end-point1", "http://localhost"),
            HttpEndpoint.of("endpoint2", "GET", "/endpoint1/{id}", "/end-point1/{id}", "http://localhost")
        ));
        HttpGateway httpGateway = HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_PORT,
            HttpGatewayRouterConfiguration.asyncRouting(request -> {
                TargetRoute<String> targetRoute = httpRouter
                    .searchRoute(request.method(), request.path())
                    .map(httpRouter::computeTargetRoute)
                    .orElse(null);
                if (targetRoute == null) {
                    return buildError(HttpResponseStatus.NOT_FOUND, "No route exists for " + request.method() + " " + request.path());
                }

                // TODO ajouter du publisher peeker via la méthode preparePeekerReques
                HttpGatewayRemoteRequest remoteRequest = httpGatewayClient.prepareRequest(request).copyBasicHeaders();
                CompletableFuture<HttpGatewayRemoteResponse> remoteResponse = httpGatewayClient.executeRemoteRequest(remoteRequest);
                // TODO ajouter du code pour convertir la réponse
                // TODO gestion responseStatus.getStatusCode() < 500
                // return httpGatewayClient.executeRemoteRequest(remoteRequest);
                return null;
            })
        ));

        HttpResponse<String> httpResponse = makeHttpRequest();

        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("Hello world !");

        httpGateway.stop();
    }

    private static HttpResponse<String> makeHttpRequest() throws IOException, InterruptedException {
        return HttpClient.newHttpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + HTTP_PORT + "/test"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }
}
