package com.coreoz;

import com.coreoz.conf.HttpGatewayConfiguration;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import play.mvc.Results;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpGatewayTest {

    @Test
    public void integration_test__verify_that_server_starts_and_is_working() throws IOException, InterruptedException {
        int httpPort = 8080;
        HttpGateway httpGateway = HttpGateway.start(new HttpGatewayConfiguration(
            httpPort,
            (router) -> router.GET("/test").routingTo((request) -> Results.ok("Hello world !")).build()
        ));

        HttpResponse<String> httpResponse = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + httpPort + "/test"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        Assertions.assertThat(httpResponse.body()).isEqualTo("Hello world !");

        httpGateway.stop();
    }

}
