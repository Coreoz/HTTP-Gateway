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
- **Downstream request**:
- **Upstream request**:
- **Client**:
- **Service**: 

Getting started and samples
---------------------------
To build a new HTTP gateway, it is best to start looking at the samples HTTP Gateways so see how it all works.

Then the steps are:
1. Create a Java project
2. Dependencies
3. Using the toolkit

TODO
----
- [ ]: add readme docs about modules, how to get started, the project motivation, use cases with custom auth, custom validation, add logging
- [ ]: upgrade play and java versions
- [ ]: provide a way to easily validate downstream request body
