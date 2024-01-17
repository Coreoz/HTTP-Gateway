HTTP Gateway Upstream Peeker
============================
Having access to request/response bodies is not easy because these bodies are supposed to be streams (using `org.reactivestreams.Publisher`) and to never be fully loaded in memory. This module provides a mechanism to peek part of these streams that are passing through the API Gateway.

This module is used mostly for logging purposes. By default, the peeking will contain a maximum of `10 000` bytes to mitigate the risk of JVM heap space overflow.

Usage
-----
The interceptor takes place as a replacement of the default upstream client, `HttpGatewayUpstreamClient` available in [the core module](../core).
The peeking interceptor client can be created this way:
```java
HttpGatewayUpstreamStringPeekerClient httpGatewayUpstreamClient = new HttpGatewayUpstreamStringPeekerClient();
```

Then it can be used exactly like `HttpGatewayUpstreamClient`:
```java
HttpGatewayPeekingUpstreamRequest<String, String> remoteRequest = httpGatewayUpstreamClient
    .prepareRequest(downstreamRequest)
    .withUrl(destinationService.getDestinationRoute().getDestinationUrl())
    .with(remoteServiceAuthenticator.forRoute(
        destinationService.getServiceId(), destinationService.getDestinationRoute().getRouteId()
    ))
    .copyBasicHeaders()
    .copyQueryParams();

CompletableFuture<HttpGatewayUpstreamKeepingResponse<String, String>> peekingUpstreamFutureResponse = httpGatewayUpstreamClient.executeUpstreamRequest(remoteRequest);

// the return statement for the HttpGatewayRouterConfiguration.asyncRouting lambda parameter
return peekingUpstreamFutureResponse.thenApply(peekingUpstreamResponse -> {
HttpGatewayUpstreamResponse upstreamResponse = peekingUpstreamResponse.getUpstreamResponse();
    // error management
    if (upstreamResponse.getStatusCode() >= HttpResponseStatus.INTERNAL_SERVER_ERROR.code()) {
        // Do not forward the response body if the upstream server returns an internal error
        // => this enables to avoid forwarding sensitive information
        PeekerPublishersConsumer.consume(upstreamResponse.getPublisher());
        // Set an empty body response
        upstreamResponse.setPublisher(null);
    }

    // The peeked request and response bodies will be available as a CompletableFuture object
    // it is because the request/response bodies are peeked only when they are read (so big content is never fully in memory)
    peekingUpstreamResponse.getStreamsPeeked().thenAccept(peekedStreams -> {
        // Peeked request/response bodies can then be logged or analyzed
        logger.debug("Proxied request: downstream={} upstream={}", peekedStreams.getDownstreamPeeking(), peekedStreams.getUpstreamPeeking());
    });
            
    return HttpGatewayDownstreamResponses.buildResult(upstreamResponse);
});
```

See `SampleBasic` in the [sample projects module](../samples) for the complete example.

Options
-------
### HttpGatewayUpstreamStringPeekerClient
This peeking client is the most common one, it is used to parse body content as string. 
The maximum number of bytes to peek is 10 000.
This number can be changed in `HttpGatewayUpstreamStringPeekerClient`:
- Inside the constructor
- Per request, using the `prepareRequest` method in the `configuration` parameter

### HttpGatewayUpstreamBytesPeekerClient
This peeking client enables to access directly the raw bytes that have passed through the API Gateway.
This client is used by the [HttpGatewayUpstreamStringPeekerClient](#httpgatewayupstreamstringpeekerclient).
This client have to be used with a configuration, see `HttpGatewayBytesStreamPeekingConfiguration` for details. This configuration enables to specify by request/response:
- The maximum number of bytes to peek
- The peeking function that is called once all that bytes have been peeked
As for the [HttpGatewayUpstreamStringPeekerClient](#httpgatewayupstreamstringpeekerclient), this configuration can be specify inside the client constructor or per request.
