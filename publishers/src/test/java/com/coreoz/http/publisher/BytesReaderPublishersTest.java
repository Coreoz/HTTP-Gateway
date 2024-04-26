package com.coreoz.http.publisher;

import com.coreoz.http.HttpGateway;
import com.coreoz.http.conf.HttpGatewayConfiguration;
import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import com.coreoz.http.mock.LocalHttpClient;
import com.coreoz.http.mock.SparkMockServer;
import com.coreoz.http.play.HttpGatewayDownstreamResponses;
import com.coreoz.http.upstream.HttpGatewayUpstreamResponse;
import com.google.common.net.HttpHeaders;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.reactivestreams.Publisher;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;

public class BytesReaderPublishersTest {
    static int HTTP_GATEWAY_PORT = 9876;

    static {
        SparkMockServer.initialize();
    }

    @Test
    public void verify_bytes_reader_publisher_is_correctly_interpreted_with_play_request() {
        HttpGateway httpGateway = HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            routerDsl -> routerDsl.addRoutes(HttpGatewayRouterConfiguration.asyncRouting(request -> {
                //noinspection unchecked
                return BytesReaderPublishers
                    .publisherToFutureBytes((Publisher< ByteBuf>) request.body().as(Publisher.class), ByteReaders::readBytesFromByteBuf)
                    .thenApply(requestBodyBytes -> {
                        HttpGatewayUpstreamResponse upstreamResponse = new HttpGatewayUpstreamResponse(request.uri());
                        upstreamResponse.setStatusCode(200);
                        upstreamResponse.setContentType(request.contentType().orElse(null));
                        upstreamResponse.setContentLength(request.header(HttpHeaders.CONTENT_LENGTH).orElse(null));
                        upstreamResponse.setPublisher(StaticBytesPublishers.makeBodyPartPublisher(requestBodyBytes));
                        return HttpGatewayDownstreamResponses.buildResult(upstreamResponse);
                    });
            })))
        );

        String sampleBody = "Hello world, this is a sample request body";
        HttpResponse<String> httpResponse = LocalHttpClient.makeHttpRequest(
            HTTP_GATEWAY_PORT,
            "/test",
            request -> request.POST(HttpRequest.BodyPublishers.ofString(sampleBody))
        );

        Assertions.assertThat(httpResponse.statusCode()).isEqualTo(HttpResponseStatus.OK.code());
        String responseBody = httpResponse.body();
        Assertions.assertThat(responseBody).isEqualTo(sampleBody);

        httpGateway.stop();
    }

    @Test
    public void verify_bytes_reader_publisher_is_correctly_interpreted_with_ahc_response() throws ExecutionException, InterruptedException {
        String sampleBody = "Hello world, this is another sample request body";
        byte[] responseBytes = UpstreamRequests
            .executeEchoRequest(sampleBody.getBytes())
            .thenCompose(response -> BytesReaderPublishers.publisherToFutureBytes(response.getPublisher(), ByteReaders::readBytesFromHttpResponseBodyPart))
            .get();
        Assertions.assertThat(responseBytes).asString().isEqualTo(sampleBody);
    }

    @Test
    public void verify_bytes_reader_publisher_is_correctly_interpreted_with_ahc_response_and_limit_size() throws ExecutionException, InterruptedException {
        long bodySize = 100L;
        byte[] sampleBody = PublisherBytes.makeBytes((int) bodySize);
        byte[] responseBytes = UpstreamRequests
            .executeEchoRequest(sampleBody)
            .thenCompose(response -> BytesReaderPublishers.publisherToFutureBytes(response.getPublisher(), ByteReaders::readBytesFromHttpResponseBodyPart, bodySize))
            .get();
        Assertions.assertThat(responseBytes).isEqualTo(sampleBody);
    }

    @Test
    public void verify_bytes_reader_publisher_throws_exception_with_ahc_response_and_just_over_limit_size() throws InterruptedException {
        try {
            long bodySize = 100L;
            byte[] sampleBody = PublisherBytes.makeBytes((int) bodySize);
            UpstreamRequests
                .executeEchoRequest(sampleBody)
                .thenCompose(response -> BytesReaderPublishers.publisherToFutureBytes(response.getPublisher(), ByteReaders::readBytesFromHttpResponseBodyPart, bodySize - 1))
                .get();
            Assertions.fail("Excepted exception because the limite has been raised");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException) {
                Assertions.assertThat(e.getCause().getMessage()).isEqualTo("Tried to read at least 100 bytes of data whereas the limit was set as 99");
            } else {
                Assertions.fail("Excepted RuntimeException");
            }
        }

    }
}
