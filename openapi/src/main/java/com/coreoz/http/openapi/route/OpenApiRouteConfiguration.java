package com.coreoz.http.openapi.route;

import com.coreoz.http.openapi.fetching.OpenApiFetcher;
import com.coreoz.http.services.HttpGatewayRemoteServicesIndex;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

@Getter
@Setter
public class OpenApiRouteConfiguration {
    private @NotNull String routePath = "/openapi";
    private @NotNull OpenAPI baseOpenApi = new OpenAPI();
    // TODO finish add these OpenApi merger parameters
    private @NotNull Function<String, String> componentNamePrefixMaker = StringUtils::capitalize;
    private @NotNull Function<String, String> operationIdPrefixMaker = Function.identity();
    private boolean createMissingEndpoints = true;
    private final @NotNull List<? extends OpenApiFetcher> openApiFetchers;
    private final @NotNull HttpGatewayRemoteServicesIndex remoteServicesIndex;

    public OpenApiRouteConfiguration(@NotNull List<? extends OpenApiFetcher> openApiFetchers, @NotNull HttpGatewayRemoteServicesIndex remoteServicesIndex) {
        this.openApiFetchers = openApiFetchers;
        this.remoteServicesIndex = remoteServicesIndex;
    }

    // TODO add client authentication & OpenApi filtering
}
