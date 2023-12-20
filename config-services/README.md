_HTTP Gateway Config Services
=============================
[Config module](../config/) to read services configuration:
- Available services
- Authentication for services access, if needed

Usual usage is:
```java
// Read services general configuration
HttpGatewayRemoteServicesIndex servicesIndex = HttpGatewayConfigServices.readConfig(configLoader);
// Read services authentication configuration
HttpGatewayRemoteServiceAuthenticator remoteServiceAuthenticator = HttpGatewayConfigServicesAuth.readConfig(configLoader);
```

See [remote services documentation]()(../remote-services/) for more information about the Java objects created.

Sample clients configuration
----------------------------
```hocon
remote-services = [
    {
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
    {
        service-id = "other-service"
        base-url = "https://some-other.service.com"
        # Authentication is optional
        routes = [
            {route-id = "route-sample", method = "GET", path = "/route-sample"}
        ]
    }
]

# override gateway routes
gateway-rewrite-routes = [
    {route-id = "fetch-pet", downstream-path = "/custom-pets/{id}/custom-route"}
]
```

Key points
----------
- Services id and routes id must be unique
- Authentication is optional
- For a same service, routes cannot overlap
- By default, the route created on the Gateway will be the same as the route defined for the service, in the `path` field
- To have a different route on the Gateway, `gateway-rewrite-routes` must be used to specify the Gateway endpoint
- See [the sample custom routing](../samples#custom-routing) for more complex routing cases and configuration factorization
