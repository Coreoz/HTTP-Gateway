package com.coreoz.http.upstreamauth;

import org.asynchttpclient.RequestBuilder;
import play.mvc.Http;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface HttpGatewayUpstreamAuthenticator extends BiConsumer<Http.Request, RequestBuilder> {
}
