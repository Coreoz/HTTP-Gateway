HTTP Gateway Router
===================
This module provides an indexed routing system for an endpoint defined by:
```java
public class HttpEndpoint {
  String routeId;
  String method;
  String downstreamPath;
  String upstreamPath;
}
```

This router functions with path pattern with the form of `{pattern}`.
A pattern cannot contain slash: `/`.
A pattern takes the place of a full path segment: `/resources/{id}` is ok, but `/resources/{id}.json` or `/resources/123-{id}/details` are not. 
Some example of valid path:
- `/resources/{id}`
- `/resources/{id}/{other-pattern}`
- `/resources/{id}/specific-route`: note that in that case, this route will have priority compared to the route `/resources/{id}/{other-pattern}`

The router will handle correctly downstream path and upstream path where the pattern order is not the same. Matching of the pattern names is however important.
So:
- Correct example: `{routeId: 'routeA', method: 'GET', downstreamPath: '/resources/{id}/specific-route', upstreamPath: '/some-prefix/specific-route-for-resources/{id}'}`
- Incorrect example: `{routeId: 'routeA', method: 'GET', downstreamPath: '/resources/{id}/specific-route', upstreamPath: '/resources/{other-id-name}/specific-route'}`

The entry point is `HttpGatewayRouter`, which provides:
- A constructor that index endpoints, of type`HttpEndpoint`
- A method to search a route in the index, by method and downstream path
- A method to convert a route found in the index, of type `MatchingRoute`, to a full destination route with the correct parameters
