HTTP Gateway samples
====================

Sample projets that uses most of the Gateway components

Basic HTTP Gateway
------------------
This example shows a HTTP Gateway with:
- Clients authentication with API key
- Client access control to services by:
  - List of routes
  - List of services
  - List of groups of routes
- Remote services with and without authentication
- Rewrite path on the Gateway for a specific service endpoint
- Downstream and upstream logging

The example is composed of:
- The HTTP Gateway class: `SampleBasic`
- The configuration file `application.conf`, which is the default configuration file used
- The integration test class (with a mock server): `SampleBasicTest`

Note that it is possible to override config property per environment. For example to override the base URL of a service depending on the environment, it is possible to have:
- The base config file `application.conf` containing:
```hocon
test-service = {
  service-id = "test-service"
  base-url = "http://localhost:4567"
  auth = {
    type = "basic"
    userId = "test-auth"
    password = "auth-password"
  }
  routes = [
    {route-id = "fetch-pets", method = "GET", path = "/pets"}
    {route-id = "fetch-pet", method = "GET", path = "/pets/{id}"}
    {route-id = "fetch-pet-friends", method = "GET", path = "/pets/{id}/friends"}
    {route-id = "add-pet", method = "POST", path = "/pets"}
  ]
}

http-gateway = {
    remote-services = [
        ${test-service}
        {
            service-id = "other-service"
            base-url = "http://localhost:4567/other-service"
            routes = [
                {route-id = "route-sample", method = "GET", path = "/route-sample"}
            ]
        }
    ]
    # ...
}
```
- The staging config file `staging.conf` containing:
```hocon
include classpath("application.conf")

http-gateway = {
    remote-services = [
        ${test-service} {
            base-url = "https://staging.test-service.com"
        }
    ]
}
```

For more information about config loading, see the [Config library file loading behavior documentation](https://github.com/lightbend/config#standard-behavior).

Moreover, config resolution can be debugged this way:
```java
ConfigRenderOptions options = ConfigRenderOptions
  .defaults()
  .setJson(false)           // false: HOCON, true: JSON
  .setOriginComments(false) // true: add comment showing the origin of a value
  .setComments(true)        // true: keep original comment
  .setFormatted(true);       // true: pretty-print result
System.out.println(configLoader.getHttpGatewayConfig().root().render(options));
```

Custom client dimension
-----------------------
This example has everything from the [basic sample](#basic-http-gateway) and shows how to restrict clients by a custom dimension.
In this example, this dimension is represented by the notion of "tenant":
- A client is associated with one or multiple tenants in the configuration file
- The HTTP Gateway verifies that clients:
  - Supply the custom header `X-Tenants` with the name/id of the tenant
  - That the supplied tenant is associated to the client in the configuration file
- The HTTP Gateway then forwards this custom header value to the remote upstream services: this way remote upstream services can use the tenant value and consider it safe to use to query/update data (with this hypothesis that only the Gateway can access the services, and of course there is not security...) 

The example is composed of:
- The HTTP Gateway class: `SampleCustomClientDimension`
- The configuration file `custom-client-dimension.conf`
- The integration test class (with a mock server): `SampleCustomClientDimensionTest`

Custom routing
--------------
This example shows how to implement custom routing based on a custom dimension added to a client:
- Clients shares a common routing
- Depending on the custom dimension, here `customer-a` or `customer-b`, other routes available are different, and for the same path on the Gateway, different remote services are queried

The example is composed of:
- The HTTP Gateway class: `SampleCustomRouting`
- The configuration file `custom-routing.conf`
- The integration test class (with a mock server): `SampleCustomRoutingTest`

Header forwarding using config
------------------------------
A common use case is to forward some headers from downstream requests to upstream services.
This can be done easily. For example:
In the config file, define the headers to be forwarded:
```hocon
http-gateway = {
    // ...
    headers-to-forward = ["Cookie", "X-Custom-Header"]
    // ...
}
```

In the API Gateway client, custom config values can be read using the `configLoader` object (or directly the `config` object if available): `configLoader.getHttpGatewayConfig().getStringList("headers-to-forward")`

So the complete request call will be written this way:
```java
HttpGatewayPeekingUpstreamRequest<String, String> remoteRequest = httpGatewayUpstreamClient
    .prepareRequest(downstreamRequest)
    .withUrl(destinationService.getDestinationRoute().getDestinationUrl())
    .with(remoteServiceAuthenticator.forRoute(
    destinationService.getServiceId(), destinationService.getDestinationRoute().getRouteId()
    ))
    .copyBasicHeaders()
    // Forward config based headers
    .copyHeaders(configLoader.getHttpGatewayConfig().getStringList("headers-to-forward").toArray(String[]::new))
    .copyQueryParams();
```
