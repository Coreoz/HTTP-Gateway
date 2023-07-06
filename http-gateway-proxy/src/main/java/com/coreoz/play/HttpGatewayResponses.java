package com.coreoz.play;

import com.coreoz.play.responses.JsonContent;
import com.coreoz.play.responses.ResultError;
import io.netty.handler.codec.http.HttpResponseStatus;
import play.mvc.Result;
import play.mvc.Results;

import java.util.concurrent.CompletableFuture;

public class HttpGatewayResponses {
    public static CompletableFuture<Result> buildError(HttpResponseStatus status, String errorMessage) {
        return CompletableFuture.completedFuture(Results.status(
            status.code(),
            new JsonContent(new ResultError(errorMessage))
        ));
    }
}
