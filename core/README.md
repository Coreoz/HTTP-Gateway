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

See the [publishers module](../publishers) to view/manipulate the request/response payload.

Downstream server configuration
-------------------------------
Downstream server relies on [Play Server](https://www.playframework.com/documentation/3.0.x/JavaEmbeddingPlay), which uses [Netty](https://github.com/netty/netty) under the hood.
Moreover, Play Server relies on the [Config library](https://github.com/typesafehub/config) to read configuration files: this library is also used by the [Config module](../config/) that contains usage details.

Netty customizations can be specified in the configuration file.
All available Netty configuration values are listed on the [Play Server documentation page](https://www.playframework.com/documentation/3.0.x/SettingsNetty#configuring-netty-server-backend).

**On production**, on Linux server, it is recommended to use enable the Netty native transport to have the best throughput:
```hocon
play.server.netty.transport = "native"
```

Upstream client configuration
-----------------------------
The upstream client relies on [Async HTTP Client](https://github.com/AsyncHttpClient/async-http-client), which uses [Netty](https://github.com/netty/netty) under the hood.

To customize upstream timeouts or other parameters, it is possible to use the two constructors in `HttpGatewayUpstreamClient`:
- `new HttpGatewayUpstreamClient(Duration.ofMinutes(1), Duration.ofMinutes(1))`: to change only the basic timeouts 
- `new HttpGatewayUpstreamClient(new DefaultAsyncHttpClientConfig.Builder().setRequestTimeout(60_000))`: to change all available AHC configuration values, see `DefaultAsyncHttpClientConfig` or `AsyncHttpClientConfig` for available values.

By defaults, request and read timeouts are set to 5 minutes.
If all upstream endpoints respond in less than 10 seconds, it is recommended to reduce these default timeouts.  
