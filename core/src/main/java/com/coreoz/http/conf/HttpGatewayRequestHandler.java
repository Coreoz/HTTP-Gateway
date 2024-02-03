package com.coreoz.http.conf;

import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

/**
 * The function that will handle all HTTP Gateway requests
 */
@FunctionalInterface
public interface HttpGatewayRequestHandler {
    CompletionStage<Result> handleRequest(Http.Request request);
}
