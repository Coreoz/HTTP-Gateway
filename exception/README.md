HTTP Gateway Exception
======================
This module defines the `HttpGatewayValidationException` exception that is thrown when there is a configuration/parameter that is incorrect.

This means that this exception must be raised only during server startup, it must never be thrown during any proxy operation.
