HTTP Gateway OpenAPI
====================
This module enables to expose on the HTTP Gateway an endpoint that will serve a unified OpenAPI specification file for all the HTTP Gateway endpoints.
TODO link to the OpenAPI merger lib.

This module exposes a route that will respond with the unified OpenAPI specification. So for example:
- If the service `A` exposes an OpenAPI file, and the service `B` also exposes a different OpenAPI file
- And if the HTTP Gateway exposes only the routes `x` and `y` from service `A` and the route `z` from service `B`
- Then the OpenAPI route will expose a unique OpenAPI file containing only the routes `x`, `y` and `z` (even though the service contained more routes)  

Usage
-----
See [the OpenAPI configuration module](../config-openapi) and [the sample basic example](../samples#basic-http-gateway) for examples.

Configuration
-------------
See [the OpenAPI route configuration class](src/main/java/com/coreoz/http/openapi/route/OpenApiRouteConfiguration.java) for the detailed configuration options.
