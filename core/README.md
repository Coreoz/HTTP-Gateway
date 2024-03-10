HTTP Gateway Core
=================
The base module of HTTP Gateway that enables to proxy incoming downstream requests to upstream services.

Other modules are needed to add the notions of [clients](../#core-concepts) and [services](../#core-concepts), or to enables reading configuration files.

The Gateway Core module is usable as it is though, for example to proxy all incoming requests to a single host, it is possible to do:
```java
HttpGatewayUpstreamClient httpGatewayUpstreamClient = new HttpGatewayUpstreamClient();
HttpGateway httpGateway = HttpGateway.start(new HttpGatewayConfiguration(
    HTTP_GATEWAY_PORT,
    HttpGatewayRouterConfiguration.asyncRouting(request -> {
        HttpGatewayUpstreamRequest upstreamRequest = httpGatewayUpstreamClient
            .prepareRequest(request)
            .withUrl("http://remote-host.com" + request.path());
        CompletableFuture<HttpGatewayUpstreamResponse> upstreamFutureResponse = httpGatewayUpstreamClient.executeUpstreamRequest(upstreamRequest);
        return upstreamFutureResponse.thenApply(upstreamResponse -> {
            if (upstreamResponse.getStatusCode() > HttpResponseStatus.INTERNAL_SERVER_ERROR.code()) {
                // Do not forward the response body if the upstream server returns an internal error
                // => this enables to avoid forwarding sensitive information
                upstreamResponse.setPublisher(null);
            }

            return HttpGatewayDownstreamResponses.buildResult(upstreamResponse);
        });
    })
));
```
Some more samples are available in the test file `HttpGatewayTest`.

TODO provide configuration information about:
- upstream timeouts
- downstream max request body
