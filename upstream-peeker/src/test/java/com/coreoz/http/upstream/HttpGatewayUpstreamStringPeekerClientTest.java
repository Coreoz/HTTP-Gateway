package com.coreoz.http.upstream;

import com.coreoz.http.HttpGateway;
import com.coreoz.http.conf.HttpGatewayConfiguration;
import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import com.coreoz.http.mock.LocalHttpClient;
import com.coreoz.http.mock.SparkMockServer;
import com.coreoz.http.play.HttpGatewayDownstreamResponses;
import com.coreoz.http.upstream.publisher.PeekerPublishersConsumer;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class HttpGatewayUpstreamStringPeekerClientTest {
    private static final String HEADER_PEEKING_BYTES_COUNT = "X-Peeking-Bytes-Count";
    static int HTTP_GATEWAY_PORT = 9876;

    static HttpGateway httpGateway;

    static CompletableFuture<HttpGatewayUpstreamKeepingResponse.HttpGatewayUpstreamPeeked<String, String>> peakedStreams;

    @BeforeClass
    public static void prepareGateway() {
        SparkMockServer.initialize();

        HttpGatewayUpstreamStringPeekerClient httpGatewayUpstreamClient = new HttpGatewayUpstreamStringPeekerClient();
        httpGateway = HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            HttpGatewayRouterConfiguration.asyncRouting(downstreamRequest -> {

                HttpGatewayStringStreamPeekingConfiguration peekingConfig = new HttpGatewayStringStreamPeekingConfiguration(downstreamRequest
                    .header(HEADER_PEEKING_BYTES_COUNT)
                    .map(Integer::parseInt)
                    .orElse(HttpGatewayStringStreamPeekingConfiguration.DEFAULT_MAX_BYTES_TO_PEEK));

                HttpGatewayPeekingUpstreamRequest<String, String> remoteRequest = httpGatewayUpstreamClient
                    .prepareRequest(downstreamRequest, peekingConfig)
                    .withUrl("http://localhost:" + SparkMockServer.SPARK_HTTP_PORT + downstreamRequest.path())
                    .copyBasicHeaders()
                    .copyQueryParams();
                CompletableFuture<HttpGatewayUpstreamKeepingResponse<String, String>> peekingUpstreamFutureResponse = httpGatewayUpstreamClient.executeUpstreamRequest(remoteRequest);
                return peekingUpstreamFutureResponse.thenApply(peekingUpstreamResponse -> {
                    HttpGatewayUpstreamResponse upstreamResponse = peekingUpstreamResponse.getUpstreamResponse();
                    if (upstreamResponse.getStatusCode() >= HttpResponseStatus.INTERNAL_SERVER_ERROR.code()) {
                        // Do not forward the response body if the upstream server returns an internal error
                        // => this enables to avoid forwarding sensitive information
                        PeekerPublishersConsumer.consume(upstreamResponse.getPublisher());
                        upstreamResponse.setPublisher(null);
                    }

                    peakedStreams = peekingUpstreamResponse.getStreamsPeeked();

                    return HttpGatewayDownstreamResponses.buildResult(upstreamResponse);
                });
            })
        ));
    }

    @AfterClass
    public static void shutdownGateway() {
        httpGateway.stop();
    }

    private static HttpResponse<String> makeHttpRequest(String targetPath) throws IOException, InterruptedException {
        return makeHttpRequest(targetPath, null);
    }

    private static HttpResponse<String> makeHttpRequest(String targetPath, String requestBody) throws IOException, InterruptedException {
        return makeHttpRequest(targetPath, requestBody, null);
    }

    private static HttpResponse<String> makeHttpRequest(String targetPath, String requestBody, Integer maxBytesToPeek) throws IOException, InterruptedException {
        return LocalHttpClient.makeHttpRequest(HTTP_GATEWAY_PORT, targetPath, requestBuilder -> {
            if (requestBody != null) {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody));
            } else {
                requestBuilder.GET();
            }

            if (maxBytesToPeek != null) {
                requestBuilder.header(HEADER_PEEKING_BYTES_COUNT, maxBytesToPeek.toString());
            }

            return requestBuilder;
        });
    }

    @Test
    public void integration_test__verify_that_empty_downstream_and_nonempty_upstream_works() throws Exception {
        HttpResponse<String> httpResponse = makeHttpRequest("/hello");
        Assertions.assertThat(httpResponse.body()).isEqualTo("World");
        Assertions.assertThat(peakedStreams).isNotNull();
        HttpGatewayUpstreamKeepingResponse.HttpGatewayUpstreamPeeked<String, String> peekedContent = peakedStreams.get(1, TimeUnit.SECONDS);
        Assertions.assertThat(peekedContent.getDownstreamPeeking()).isNull();
        Assertions.assertThat(peekedContent.getUpstreamPeeking()).isEqualTo("World");
    }

    @Test
    public void integration_test__verify_that_nonempty_downstream_and_empty_upstream_works() throws Exception {
        HttpResponse<String> httpResponse = makeHttpRequest("/no-body-simple", "non empty downstream body");
        Assertions.assertThat(httpResponse.body()).isNullOrEmpty();
        Assertions.assertThat(peakedStreams).isNotNull();
        HttpGatewayUpstreamKeepingResponse.HttpGatewayUpstreamPeeked<String, String> peekedContent = peakedStreams.get(1, TimeUnit.SECONDS);
        Assertions.assertThat(peekedContent.getDownstreamPeeking()).isEqualTo("non empty downstream body");
        Assertions.assertThat(peekedContent.getUpstreamPeeking()).isNull();
    }

    @Test
    public void integration_test__verify_that_non_empty_streams_works() throws Exception {
        HttpResponse<String> httpResponse = makeHttpRequest("/long-body", "long request body with many characters");
        Assertions.assertThat(httpResponse.body()).isEqualTo("This is a loooooooooooooooooong body");
        Assertions.assertThat(peakedStreams).isNotNull();
        HttpGatewayUpstreamKeepingResponse.HttpGatewayUpstreamPeeked<String, String> peekedContent = peakedStreams.get(1, TimeUnit.SECONDS);
        Assertions.assertThat(peekedContent.getDownstreamPeeking()).isEqualTo("long request body with many characters");
        Assertions.assertThat(peekedContent.getUpstreamPeeking()).isEqualTo("This is a loooooooooooooooooong body");
    }

    @Test
    public void integration_test__verify_that_upstream_error_response_not_read_is_still_traced() throws Exception {
        HttpResponse<String> httpResponse = makeHttpRequest("/server-error", "Request error");
        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(500);
        Assertions.assertThat(httpResponse.body()).isNullOrEmpty();
        Assertions.assertThat(peakedStreams).isNotNull();
        HttpGatewayUpstreamKeepingResponse.HttpGatewayUpstreamPeeked<String, String> peekedContent = peakedStreams.get(1, TimeUnit.SECONDS);
        Assertions.assertThat(peekedContent.getUpstreamPeeking()).isEqualTo("Internal server error!");
    }

    @Test
    public void integration_test__verify_that_peeking_size_is_correctly_used() throws Exception {
        HttpResponse<String> httpResponse = makeHttpRequest("/long-body", "long request body with many characters", 10);
        Assertions.assertThat(httpResponse.body()).isEqualTo("This is a loooooooooooooooooong body");
        Assertions.assertThat(peakedStreams).isNotNull();
        HttpGatewayUpstreamKeepingResponse.HttpGatewayUpstreamPeeked<String, String> peekedContent = peakedStreams.get(1, TimeUnit.SECONDS);
        Assertions.assertThat(peekedContent.getDownstreamPeeking()).isEqualTo("long reque");
        Assertions.assertThat(peekedContent.getUpstreamPeeking()).isEqualTo("This is a ");
    }
}
