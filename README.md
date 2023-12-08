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
TODO diagrams & description

Core concepts are important to correctly use and understand HTTP Gateway:
- **Downstream request**: An incoming HTTP request to the HTTP Gateway, made by a client
- **Upstream request**: An HTTP made from the HTTP Gateway to a service, in response to a downstream request
- **Client**: A system that will make HTTP requests to the HTTP Gateway
- **Service**: A system that will be made available through and on the HTTP Gateway

Getting started and samples
---------------------------
To build a new HTTP gateway, it is best to start looking at the [samples HTTP Gateways](samples/) so see how it all works.

Then the steps are:
1. Create a Java project, for example using the [Plume archetype](https://github.com/Coreoz/Plume-archetpes)
2. Make sure to use at least Java 11
3. Add the HTTP Gateway Maven dependencies, in doubt it is possible to copy the ones from the [sample HTTP Gateways pom.xml file](samples/pom.xml) 
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

### Remote services


### Upstream authentication
### Upstream peeker
### Client access control
### Downstream validation
### Config
### Test

Modules dependency tree:

TODO
----
- [ ]: add readme docs about modules, how to get started, the project motivation, use cases with custom auth, custom validation, add logging
- [ ]: upgrade play and java versions
- [ ]: provide a way to easily validate downstream request body
