HTTP Gateway Upstream Authentication
====================================
This module defines the `HttpGatewayUpstreamAuthenticator` interface and all the upstream authenticators.

This `HttpGatewayUpstreamAuthenticator` interface is used in the `HttpGatewayUpstreamClient` to query an upstream service (using the `HttpGatewayRequestCustomizer` interface) :
- See [HTTP Gateway core module](../core) for details about the `HttpGatewayUpstreamClient` or `HttpGatewayRequestCustomizer`
- See [HTTP Gateway samples module](../samples) for usage examples (using the `HttpGatewayRemoteServiceAuthenticator` adapter from the [remote services module](../remote-services) to find the authenticator for a service)

Supported upstream authentications
----------------------------------
Supported upstream authentications are:
- Basic authentication: See `HttpGatewayRemoteServiceBasicAuthenticator`
- API key authentication: See `HttpGatewayRemoteServiceKeyAuthenticator`

The currently supported authentications is poor because we did not have yet uses cases for other authentications. Though the `HttpGatewayUpstreamAuthenticator` interface should be simple enough to easily provide other custom authentications if needed. If you implement a standard authentication method, please provide a PR :)
Custom authentications can be passed when using `HttpGatewayConfigServicesAuth` when calling the `readConfig` method and passing supported authenticators as the second argument.
