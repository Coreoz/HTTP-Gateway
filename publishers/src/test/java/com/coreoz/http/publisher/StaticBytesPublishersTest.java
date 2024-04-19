package com.coreoz.http.publisher;

import com.coreoz.http.HttpGateway;
import com.coreoz.http.conf.HttpGatewayConfiguration;
import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import com.coreoz.http.mock.SparkMockServer;
import com.coreoz.http.play.HttpGatewayDownstreamResponses;
import com.coreoz.http.upstream.HttpGatewayResponseStatus;
import com.coreoz.http.upstream.HttpGatewayUpstreamClient;
import com.coreoz.http.upstream.HttpGatewayUpstreamRequest;
import com.coreoz.http.upstream.HttpGatewayUpstreamResponse;
import com.google.common.net.HttpHeaders;
import com.google.common.primitives.Bytes;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import play.mvc.Http;

import java.net.http.HttpResponse;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.coreoz.http.mock.LocalHttpClient.makeHttpGetRequest;

public class StaticBytesPublishersTest {
    static int HTTP_GATEWAY_PORT = 9876;

    static {
        SparkMockServer.initialize();
    }

    @Test
    public void verify_static_bytes_publisher_is_correctly_interpreted_with_play_response() {
        HttpGateway httpGateway = HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            routerDsl -> routerDsl.addRoutes(HttpGatewayRouterConfiguration.asyncRouting(request -> {
                HttpGatewayUpstreamResponse upstreamResponse = new HttpGatewayUpstreamResponse(request.uri());
                upstreamResponse.setStatusCode(200);
                upstreamResponse.setContentType(request.contentType().orElse(null));
                upstreamResponse.setContentLength(request.header(HttpHeaders.CONTENT_LENGTH).orElse(null));
                upstreamResponse.setPublisher(StaticBytesPublishers.makeBodyPartPublisher(makeBytes()));
                return CompletableFuture.completedFuture(HttpGatewayDownstreamResponses.buildResult(upstreamResponse));
            })))
        );

        HttpResponse<String> httpResponse = makeHttpGetRequest(
            HTTP_GATEWAY_PORT,
            "/test"
        );

        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        String responseBody = httpResponse.body();
        Assertions.assertThat(responseBody).startsWith("start");
        Assertions.assertThat(responseBody).endsWith("end");
        Assertions.assertThat(responseBody.getBytes().length).isGreaterThan(8_000_000);

        httpGateway.stop();
    }

    @Test
    public void verify_static_bytes_publisher_is_correctly_interpreted_with_ahc_request() throws ExecutionException, InterruptedException {
        HttpGatewayUpstreamClient upstreamClient = new HttpGatewayUpstreamClient();
        HttpGatewayUpstreamRequest request = upstreamClient
            .prepareRequest(new Http.RequestBuilder().build())
            .withUrl("http://localhost:" + SparkMockServer.SPARK_HTTP_PORT + "/echo")
            .with(((downstreamRequest, upstreamRequest) -> {
                upstreamRequest.setBody(StaticBytesPublishers.makeByteBufPublisher(makeBytes()));
                upstreamRequest.setMethod("POST");
            }));

        HttpGatewayUpstreamResponse response = upstreamClient
            .executeUpstreamRequest(request)
            .get();

        Assertions.assertThat(response.getResponseStatus()).isEqualTo(HttpGatewayResponseStatus.OK);
        Assertions.assertThat(response.getPublisher()).isNotNull();
        byte[] bodyBytes = BytesReaderPublishers.publisherToFutureBytes(response.getPublisher(), ByteReaders::readBytesFromHttpResponseBodyPart, 10_000_000L).get();
        Assertions.assertThat(bodyBytes.length).isGreaterThan(8_000_000);
        String bodyString = new String(bodyBytes);
        Assertions.assertThat(bodyString).startsWith("start");
        Assertions.assertThat(bodyString).endsWith("end");
    }

    private byte[] makeBytes() {
        Random random = new Random();
        byte[] randomBytes = new byte[8_000_000];
        random.nextBytes(randomBytes);

        return Bytes.concat(
            "start".getBytes(),
            randomBytes,
            "end".getBytes()
        );
    }
}
