package com.coreoz.http.openapi.route;

import com.coreoz.http.openapi.fetching.OpenApiFetcher;
import com.coreoz.http.services.HttpGatewayRemoteServicesIndex;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

/**
 * The OpenAPI route configuration.<br>
 * <br>
 * See {@link OpenApiRoute} and {@link OpenApiOrchestrator}
 */
@Getter
@Setter
@Accessors(fluent = true)
public class OpenApiRouteConfiguration {
    /**
     * The route path on the HTTP Gateway that will be used to configure the OpenAPI route
     */
    private @NotNull String routePath = "/openapi";
    /**
     * The base OpenAPI definition in which all other OpenAPI definitions will be merged
     */
    private @NotNull OpenAPI baseOpenApi = new OpenAPI();
    /**
     * The function used to prefix component names when they are being merged into the base OpenAPI definition.<br>
     * This is used to avoid having component name conflict.<br>
     * <br>
     * The function takes the HTTP Gateway Service ID as a parameter and returns the prefix.<br>
     * <br>
     * To not have any prefix and to not change component names, use the function: <code>() -> ""</code>
     */
    private @NotNull Function<String, String> componentNamePrefixMaker = StringUtils::capitalize;
    /**
     * The function used to prefix operation names when they are being merged into the base OpenAPI definition.<br>
     * This is used to avoid having operation name conflict.<br>
     * <br>
     * The function takes the HTTP Gateway Service ID as a parameter and returns the prefix.<br>
     * <br>
     * To not have any prefix and to not change component names, use the function: <code>() -> ""</code>
     */
    private @NotNull Function<String, String> operationIdPrefixMaker = Function.identity();
    // TODO to implements
    private boolean createMissingEndpoints = true;
    /**
     * The {@link OpenApiFetcher}s used to retrieve service OpenAPI definitions
     */
    private final @NotNull List<? extends OpenApiFetcher> openApiFetchers;
    /**
     * The HTTP Gateway Services index
     */
    private final @NotNull HttpGatewayRemoteServicesIndex remoteServicesIndex;

    public OpenApiRouteConfiguration(@NotNull List<? extends OpenApiFetcher> openApiFetchers, @NotNull HttpGatewayRemoteServicesIndex remoteServicesIndex) {
        this.openApiFetchers = openApiFetchers;
        this.remoteServicesIndex = remoteServicesIndex;
    }

    // TODO add client authentication & OpenApi filtering
}
