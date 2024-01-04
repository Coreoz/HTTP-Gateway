HTTP Gateway Downstream Validation
==================================
This module defines for downstream validation:
- The validation monad `HttpGatewayValidation` to help chain validations
- Common validators using `HttpGatewayValidation`

Sample usage:
```java
// create validation objects (thread safe)
HttpGatewayClientValidator clientValidator = new HttpGatewayClientValidator(servicesIndex, gatewayClients);
HttpGatewayRouteValidator routeValidator = new HttpGatewayRouteValidator(httpRouter, servicesIndex);
// [...]
// inside HttpGatewayRouterConfiguration.asyncRouting:
// validation
HttpGatewayValidation<HttpGatewayDestinationService> validation = clientValidator
    .validateClientIdentification(downstreamRequest)
    .then(clientId -> routeValidator
        .validate(downstreamRequest)
        .then(destinationRoute -> clientValidator.validateClientAccess(downstreamRequest, destinationRoute, clientId))
    );
// error management
if (validation.isError()) {
    logger.warn(validation.error().getMessage());
    return HttpGatewayDownstreamResponses.buildError(validation.error());
}
HttpGatewayDestinationService destinationService = validation.value();
```

Custom validators
-----------------
A complete example showing how to use a custom validator is available in the sample projet, in [the custom dimension sample](../samples#custom-client-dimension).

To create a custom validator:
1. Define optionally the class that will contain the resulting data. For example:
```java
@Value
static class CustomClientValidation {
    HttpGatewayDestinationService destinationService; // the route validation result
    CustomAttributes customAttributes; // The custom data resolved with the custom validator
}
```
2. Define the validation function that will use the parameters previously resolved. For example:
```java
private static HttpGatewayValidation<CustomClientValidation> validateCustomClientAuthorization(
    Http.Request downstreamRequest,
    CustomClientAttributes clientCustomAttributes,
    HttpGatewayDestinationService destinationService
) {
    // Fetch custom downstream header values
    String[] requestedTenants = downstreamRequest.header(HTTP_HEADER_TENANTS).map(tenants -> tenants.split(",")).orElse(null);
    if (requestedTenants == null) {
        return HttpGatewayValidation.ofError(HttpResponseStatus.PRECONDITION_FAILED, "Missing header " + HTTP_HEADER_TENANTS);
    }

    // Verify client access to the requested values
    for (String requestedTenant : requestedTenants) {
        if (!clientCustomAttributes.getTenants().contains(requestedTenant)) {
            return HttpGatewayValidation.ofError(HttpResponseStatus.UNAUTHORIZED, "Access to tenant '" + requestedTenant + "' is not allowed. Allowed tenants are: " + clientCustomAttributes.getTenants());
        }
    }

    // return success validation with the custom value
    return HttpGatewayValidation.ofValue(new CustomClientValidation(
        destinationService,
        new CustomClientAttributes(Set.of(requestedTenants))
    ));
}
```
3. Use the custom validator:
```java
// custom validation
Map<String, CustomClientAttributes> clientsCustomAttributes = readClientsCustomAttributes(configLoader);

// [...]
HttpGatewayValidation<CustomClientValidation> validation = clientValidator
    .validateClientIdentification(downstreamRequest)
    .then(clientId -> routeValidator
        .validate(downstreamRequest)
        .then(destinationRoute -> clientValidator.validateClientAccess(downstreamRequest, destinationRoute, clientId))
        // custom validation
        .then(destinationService -> validateCustomClientAuthorization(
            downstreamRequest, clientsCustomAttributes.get(clientId), destinationService
        ))
    );
```
