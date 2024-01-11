HTTP Gateway Remote Services
============================
This module defines objects used for remote services usage. It contains:
- [Service definition](#service-definition)
- [Service authentication](#service-authentication): this notion is completely separated from the service definition to make it easier to customize if needed

Service definition
------------------
`HttpGatewayRemoteService` and `HttpGatewayRemoteServiceRoute` are the base object for services: they define the notions of services and routes inside a service.

`HttpGatewayRewriteRoute` defines the customization of the downstream path for a specific route.
For example, if a route is defined with `{route-id = "add-pet", method = "POST", path = "/pets"}`:
- By default the route will be available on the API Gateway on the (downstream) path `/pets`: so it will be the same path used for the service upstream path and the API Gateway downstream path
- To change that, a rewrite route object must be defined, for example the object `{route-id = "add-pet", downstream-path = "/add-pet"}` will enable to access the service endpoint to the API Gateway downstream path `/add-pet`

Service authentication
----------------------
The service authentication defines the notion of service authentication:
- `HttpGatewayRemoteServiceAuth` is the base object that defines the mapping for an authenticator to a service. See `HttpGatewayUpstreamAuthenticator` in the [upstream authentication](../upstream-auth) module for authenticator signature
- `HttpGatewayRemoteServiceAuthenticator` enables to index service authenticators from a list of `HttpGatewayRemoteServiceAuth`: this Index will then be used to find the service matching authenticator (from the service id)
