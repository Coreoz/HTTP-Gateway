HTTP Gateway Client access control
==================================
This module defines objects used for client configuration.

These objects relatives to client are created in the [client configuration module](../config-clients).

Supported authentication methods for clients are:
- **API Key**: using the `Authorization` header and the value `Bearer <auth-key>`. See `HttpGatewayClientApiKeyAuthenticator` for implementation.
- **Basic**: using the `Authorization` header and the value `Basic <base64(login:password)>`. See `HttpGatewayClientBasicAuthenticator` for implementation.

Custom authentication methods
-----------------------------
Custom authentication methods can be added by implementing `HttpGatewayClientAuthenticator` interface. See above for example.
See the [config client module documentation](../config-clients) to see how to use the newly custom authenticator. 
