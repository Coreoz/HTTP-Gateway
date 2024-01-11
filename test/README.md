HTTP Gateway Test
=================
This module contains utilities to help test API Gateways.

There is:
- `SparkMockServer`: An HTTP server that returns mock response for multiple endpoints
- `LocalHttpClient`: An HTTP client to query an API Gateway
- `ConfigExceptionValidation`: Provide a function to test an HTTP Gateway validation function that must throw `HttpGatewayValidationException`

This module is used in most HTTP Gateway modules, but some examples on complete Gateways can be seen in the [sample projects module](../samples).
