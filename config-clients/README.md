_HTTP Gateway Config Clients
===========================
[Config module](../config/) to read clients configuration:
- Authentication
- Route access

Usual usage is:
```java
// First read services configurations to it is then possible to validate client configuration after
HttpGatewayRemoteServicesIndex servicesIndex = HttpGatewayConfigServices.readConfig(configLoader);
HttpGatewayConfigClientAccessControl gatewayClients = HttpGatewayConfigClientAccessControl
    // read clients configuration
    .readConfig(
        configLoader,
        // limit the supported authentications to reduce vulnerabilities
        List.of(HttpGatewayConfigClientAuth.KEY_AUTH)
    )
    .validateConfig(servicesIndex); // validate clients configuration with actual services available
```

See [client access control documentation]()(../client-access-control/) for more information about the Java objects created.

Sample clients configuration
----------------------------
```hocon
routes-groups = [
  {routes-group-id = "fetching-pets", routes = ["fetch-pets", "fetch-pet"]}
]

clients = [
  {
    client-id = "app-zoo"
    auth = {type = "key", value = "auth-zoo"}
    allowed-routes = ["add-pet"]
    allowed-routes-groups = ["fetching-pets"]
  }
  {
    client-id = "other-app"
    auth = {type = "key", value = "other-app-key"}
    allowed-services = ["other-service"]
  }
]
```

Concepts
--------
- Route groups: This is a group of route that will be referenced in client configuration using `allowed-routes-groups`
- Clients: The list of clients allowed to access the API Gateway
- Route restriction: A client can access only routes he has been granted access to. These access are always cumulative: if for one client, there are multiple values for `allowed-routes`, `allowed-routes-groups` and `allowed-services`, then all the routes referenced by these access will be made available for the clients.

Client authentication
---------------------
Default supported authentication methods are `key` and `basic`, see [sample](#sample-clients-configuration) for usage.

To add a new custom authenticator:
1. Create the authenticator, see the [client access control module](../client-access-control) for details
2. Create the `HttpGatewayAuthObject` object that will contain the auth data described in the configuration. See `HttpGatewayAuthBasic` for an example
3. Create the `HttpGatewayAuthConfig` object that contains the auth config key and the function to read the auth config value to create the `HttpGatewayAuthObject` object. See `HttpGatewayConfigAuth` for samples
4. Create the `HttpGatewayClientAuthConfig` that map the `HttpGatewayAuthObject` and `HttpGatewayAuthConfig` objects created previously. See `HttpGatewayConfigClientAuth` for samples
5. Use the newly authenticators:
```java
HttpGatewayConfigClientAuth customAuthenticatorConfig = ... // created following the previous steps
HttpGatewayConfigClientAccessControl gatewayClients = HttpGatewayConfigClientAccessControl
    .readConfig(
        configLoader,
        // use the custom authenticator config
        List.of(customAuthenticatorConfig)
    );
```
