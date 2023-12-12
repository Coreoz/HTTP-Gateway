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
