package com.coreoz.http.validation;

import com.coreoz.http.router.data.DestinationRoute;
import lombok.Value;

@Value
public class HttpGatewayDestinationService {
    DestinationRoute destinationRoute;
    String serviceId;
}
