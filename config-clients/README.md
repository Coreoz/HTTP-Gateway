_HTTP Gateway Config Clients
===========================
[Config module](../config/) to read clients configuration.

TODO link to access control documentation

Sample clients configuration
----------------------------
```hocon
routes-groups = [
  {routes-group-id = "fetching-pets", routes = ["fetch-pets", "fetch-pet"]}
]

clients = [
  {
    client-id = "app-zoo"
    auth = {type = "key", value = "auth-zoo"}
    allowed-routes = ["add-pet"]
    allowed-routes-groups = ["fetching-pets"]
  }
  {
    client-id = "other-app"
    auth = {type = "key", value = "other-app-key"}
    allowed-services = ["other-service"]
  }
]
```

Concepts
--------
- Route groups:
- Clients
- Route restriction
