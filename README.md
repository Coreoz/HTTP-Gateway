HTTP Gateway
============
HTTP Gateway is a toolkit to quickly build efficient and customized HTTP gateways running on the JVM. This library is developer oriented as for now it does not provide yet an UI for configuration, instead it provides file based configuration connectors.

The difference between HTTP Gateway and other Gateway framework:
- Is optimized to consume minimum RAM and CPU:
  - It can stream Go of data and thousands of request per seconds with an `XMX` of `128Mo`
  - Routes are indexed so the algorithm to match the correct route always resolves in a complexity of `O(1)`, this means that if thousands of routes are served, it will not slow down a bit the Gateway
  - Almost all the components can be customized/replaced, so this toolkit can be used for many different use cases

HTTP Gateway relies on other high performance Open Source libraries, especially:
- Netty
- Play Server
- Async HTTP Client
- Config

Core concepts
-------------
![HTTP Gateway](docs/http-gateway-concepts.svg?raw=true&sanitize=true)

Core concepts are important to correctly use and understand HTTP Gateway:
- **Downstream request**: An incoming HTTP request to the HTTP Gateway, made by a client
- **Upstream request**: An HTTP made from the HTTP Gateway to a service, in response to a downstream request
- **Client**: A system that will make HTTP requests to the HTTP Gateway
- **Service**: A system that will be made available through and on the HTTP Gateway

Getting started and samples
---------------------------
To build a new HTTP gateway, it is best to start looking at the [HTTP Gateway samples](samples/) so see how it all works.

Then the steps are:
1. Create a Java project, for example using the [Plume archetype](https://github.com/Coreoz/Plume-archetpes)
2. Make sure to use at least Java 11
3. Add the HTTP Gateway Maven dependencies, in doubt, it is possible to copy the ones from the [sample HTTP Gateways pom.xml file](samples/pom.xml) 
4. Create the Gateway entry point class, it is generally easier to copy/paste a [sample gateway class](samples/src/main/java/com/coreoz/http)
5. Use and configure available [HTTP Gateway modules](#available-modules)
6. Add the configuration file, it is generally easier to copy/paste a [sample gateway config](samples/src/main/resources)

Available modules
-----------------
### Core
The base module of HTTP Gateway that enables to proxy incoming downstream requests to upstream services.

### Router
This provides routing capabilities by:
- Indexing available routes with their downstream path and their matching upstream path 
- Enabling to search in the available routes
- Computing the upstream destination route while resolving correctly route patterns

### Auth
Defines objects used to store authentication data.

Available authentications are:
- API key:
  - This can be used in configuration for clients and services using `{type = "key", value = "api-key-value"}`
  - This is used by providing the HTTP header `Authorization` the value `Bearer api-key-value` (with the correct API key value)
- Basic:
  - This can be used in configuration for clients and services using `auth = { type = "basic", userId = "userId-value", password = "password-value"}`
  - This is used by providing the HTTP header `Authorization` the value `Basic base64(userId-value:password-value)` (with the correct values)

### Remote services
This module provides upstream services routing and authentication. This module relies on the [router module](#router).

#### Upstream authentication
This module provides connectors for authentication. Currently, supported authentication are:
- Basic
- Key

### Upstream peeker
This provides the ability to peek upstream request and response:
- Headers
- Body

This is used by default in all [HTTP Gateway samples](samples/).

### Client access control
This provides client authorization and route access control: so a client can only access routes that has been allowed. 

### Downstream validation


### Config
TODO HOCOON
### Test

Modules dependency tree:

TODO
----
- [ ]: add readme docs about modules, how to get started, the project motivation, use cases with custom auth, custom validation, add logging
- [ ]: Implement both key and basic auth for clients and services
- [ ]: upgrade play and java versions
- [ ]: provide a way to easily validate downstream request body
