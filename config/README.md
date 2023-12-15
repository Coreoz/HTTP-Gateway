HTTP Gateway Config
===================
Base module for HTTP Gateway through configuration using the [Config library](https://github.com/lightbend/config).

Main config modules are:
- [Config Services](../config-services/) for reading the [remote services](../#remote-services) configuration.
- [Config Clients](../config-clients/) for reading the [clients access control](../#client-access-control) configuration.

There is also the config authentication module that is used by the other main config modules:
- [Config Authentication](../config-auth/) for reading authentication parts for clients and services.
