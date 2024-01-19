package com.coreoz.http.play;

import com.coreoz.http.play.responses.HttpGatewayDownstreamError;
import com.coreoz.http.play.responses.JsonContent;
import com.coreoz.http.play.responses.ResultError;
import com.coreoz.http.upstream.HttpGatewayUpstreamResponse;
import com.google.common.net.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.Param;
import org.reactivestreams.Publisher;
import play.http.HttpEntity;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.StatusHeader;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class HttpGatewayDownstreamResponses {
    public static CompletableFuture<Result> buildError(HttpGatewayDownstreamError error) {
        return buildError(error.getStatus(), error.getMessage());
    }

    public static CompletableFuture<Result> buildError(HttpResponseStatus status, String errorMessage) {
        return CompletableFuture.completedFuture(Results.status(
            status.code(),
            new JsonContent(new ResultError(errorMessage))
        ));
    }

    public static Result buildResult(HttpGatewayUpstreamResponse upstreamResponse) {
        StatusHeader resultStatus = Results
            .status(upstreamResponse.getStatusCode());
        // If there is no upstream response body, just returns the headers
        if (upstreamResponse.getPublisher() == null) {
            return resultStatus.withHeaders(flattenHeaders(upstreamResponse.getResponseHeaders()));
        }

        Source<ByteString, NotUsed> responseSource = responseSource(upstreamResponse.getPublisher());
        // If the upstream response contains a content length header, forward it
        if(upstreamResponse.getContentLength() != null) {
            return resultStatus
                .sendEntity(new HttpEntity.Streamed(
                    responseSource,
                    Optional.of(Long.parseLong(upstreamResponse.getContentLength())),
                    Optional.ofNullable(upstreamResponse.getContentType())
                ))
                .withHeaders(flattenHeaders(upstreamResponse.getResponseHeaders()));
        }

        // If there is no content length in the upstream response header, returns a chunked response
        Result result = resultStatus.chunked(responseSource);
        if(upstreamResponse.getContentType() != null) {
            result = result.as(upstreamResponse.getContentType());
        }
        return result
            .withHeaders(flattenHeaders(upstreamResponse.getResponseHeaders()));
    }

    private static String[] flattenHeaders(List<Param> responseHeaders) {
        return responseHeaders
            .stream()
            // content type and content length headers are added automatically by Play
            // => so these headers need to be removed
            .filter(header ->
                !HttpHeaders.CONTENT_LENGTH.equals(header.getName())
                    && !HttpHeaders.CONTENT_TYPE.equals(header.getName())
            )
            .flatMap(header -> Stream.of(header.getName(), header.getValue()))
            .toArray(String[]::new);
    }

    private static Source<ByteString, NotUsed> responseSource(Publisher<HttpResponseBodyPart> publisher) {
        return Source
            .fromPublisher(publisher)
            .map(bodyPart ->
                ByteString.fromArray(bodyPart.getBodyPartBytes())
            );
    }
}
