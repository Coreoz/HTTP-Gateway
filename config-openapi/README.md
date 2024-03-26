HTTP Gateway Config OpenAPI
===========================
This module enables to read the OpenAPI configuration to initialize the OpenAPI route from the [OpenAPI module](../openapi/).

To initialize an OpenAPI route, `OpenApiRoute`, the mandatory parameter `OpenApiRouteConfiguration` has to be provided.
This route configuration has two mandatory parameters:
- A list of OpenAPI fetchers, `OpenApiFetcher`: each fetcher will be in charge of loading the OpenAPI definition for a single service
- The service index usually created at the beginning

A sample usage is:
```java
// Usual HTTP Gateway initializations
HttpGatewayConfigLoader configLoader = new HttpGatewayConfigLoader();
HttpGatewayRemoteServicesIndex servicesIndex = HttpGatewayConfigServices.readConfig(configLoader);
// Other usual HTTP Gateway initializations
// ...

// OpenApi configuration
OpenApiRoute openApiRoute = new OpenApiRoute(new OpenApiRouteConfiguration(
    // Creation of OpenAPI fetchers from configuration
    HttpGatewayConfigOpenApiServices.readConfig(new HttpGatewayConfigOpenApiServicesParameters(configLoader)),
    servicesIndex
));
```

In this sample example, we see the usage of `HttpGatewayConfigOpenApiServices`. This is the main class provided by this module.

A complete usage sample is available in the [the sample basic example](../samples#basic-http-gateway) .

Configuration
-------------
The OpenAPI configuration is included directly in each service configuration object.
If this OpenAPI configuration is absent, then the service OpenAPI definition will be ignored.

Here is a sample OpenAPI configuration inside a service:
```hocon
{
  service-id = "test-service"
  base-url = "http://localhost:4567"
  auth = { ... }
  open-api = {
    # supported type: remote-url, remote-path, resource-path
    type = "remote-path"
    # relative path to the service base URL, so here, the complete openAPI URL will be: http://localhost:4567/openapi 
    remote-path = "/openapi"
    # the optional authentication configuration to pass the authorization to access the OpenAPI URL 
    auth = {
      type = "basic"
      userId = "test-auth"
      password = "auth-password"
    }
  }
  routes = [ ... ]
}
```

For now, these are the supported fetching configuration:
- **type = "remote-path"**: in that case the `remote-path` attribute must be filled, it will be used to create the complete OpenAPI URL using the service base URL. The `auth` object attribute can optionally be filled if the OpenAPI URL is protected by an authentication mechanism.
- **type = "remote-url"**: in that case the `remote-url` attribute must be filled, it should link to a valid OpenAPI specification file. The `auth` object attribute can optionally be filled if the OpenAPI URL is protected by an authentication mechanism.
- **type = "resource-path"**: in that case the `resource-path` attribute must be filled, it should link to a valid OpenAPI specification file copied directly in the Java/JVM HTTP Gateway project resources folder. This can be useful when the OpenAPI specification is difficult to access for a service. The `auth` object attribute should not be filled in that case.
