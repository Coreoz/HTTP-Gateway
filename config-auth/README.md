HTTP Gateway Authentication config
==================================
Technical [config module](../config/) to read the `auth` part of clients and services configuration.

It can read basic and key based configuration.
For example:
```hocon
auth = {type = "basic", userId = "test-auth", password = "auth-password"}
```

```hocon
auth = {type = "key", value = "auth-key"}
```
