package com.coreoz.client;

import io.netty.handler.codec.http.HttpHeaders;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.Param;
import org.asynchttpclient.handler.StreamedAsyncHandler;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Handle async HTTP response from Netty
 */
public class HttpGatewayClientResponseHandler implements StreamedAsyncHandler<Void> {
	private final CompletableFuture<HttpGatewayRemoteResponse> futureResponse;

	private final HttpGatewayRemoteResponse responseResult;

	public HttpGatewayClientResponseHandler(CompletableFuture<HttpGatewayRemoteResponse> futureResponse, String requestUrl) {
		this.futureResponse = futureResponse;
		this.responseResult = new HttpGatewayRemoteResponse();
		this.responseResult.setRequestUrl(requestUrl);
		responseResult.setResponseHeaders(new ArrayList<>());
	}

	// traitement de la réponse

	@Override
	public void onThrowable(Throwable t) {
		responseResult.setStatusCode(500);

		if(t instanceof TimeoutException) {
			responseResult.setResponseStatus(HttpGatewayResponseStatus.REMOTE_TIMEOUT);
		} else {
			responseResult.setResponseStatus(HttpGatewayResponseStatus.HTTP_GATEWAY_ERROR);
			responseResult.setGatewayError(t);
		}

		completeFutureResponse();
	}

	@Override
	public State onStatusReceived(HttpResponseStatus responseStatus) {
        responseResult.setStatusCode(responseStatus.getStatusCode());

		if (responseStatus.getStatusCode() < 400) {
			responseResult.setResponseStatus(HttpGatewayResponseStatus.OK);
		} else if (responseStatus.getStatusCode() < 500) {
			responseResult.setResponseStatus(HttpGatewayResponseStatus.REMOTE_CLIENT_ERROR);
		} else {
			responseResult.setResponseStatus(HttpGatewayResponseStatus.REMOTE_SERVER_ERROR);
		}

		return State.CONTINUE;
	}

	@Override
	public State onHeadersReceived(HttpHeaders headers) {
		for(Entry<String, String> header : headers) {
			if(com.google.common.net.HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(header.getKey())) {
				responseResult.setContentLength(header.getValue());
			}
			else if(com.google.common.net.HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(header.getKey())) {
				responseResult.setContentType(header.getValue());
			}
			responseResult.getResponseHeaders().add(new Param(header.getKey(), header.getValue()));
		}

		return State.CONTINUE;
	}

	@Override
	public State onStream(Publisher<HttpResponseBodyPart> publisher) {
		responseResult.setPublisher(publisher);

		completeFutureResponse();

		return State.CONTINUE;
	}

	@Override
	public State onBodyPartReceived(HttpResponseBodyPart bodyPart) {
		throw new RuntimeException("BodyPart received instead of a publisher");
	}

	@Override
	public Void onCompleted() {
		completeFutureResponse();

		return null;
	}

	// construction du résultat

	private void completeFutureResponse() {
		futureResponse.complete(responseResult);
	}
}
