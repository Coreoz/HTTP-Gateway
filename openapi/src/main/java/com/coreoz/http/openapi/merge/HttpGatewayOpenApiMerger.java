package com.coreoz.http.openapi.merge;

import com.coreoz.http.routes.HttpRoutes;
import com.coreoz.http.routes.parsing.ParsedPath;
import com.coreoz.http.routes.parsing.ParsedSegment;
import com.coreoz.openapi.OpenApiMerger;
import com.coreoz.openapi.config.OpenApiMergerConfigurationActions;
import com.coreoz.openapi.config.OpenApiOnMissingOperation;
import com.coreoz.openapi.config.OpenApiOperationAction;
import io.swagger.v3.oas.models.OpenAPI;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class HttpGatewayOpenApiMerger {
    public static OpenAPI merge(OpenAPI consolidatedSpec, OpenAPI specToMerge, OpenApiMergerConfiguration configuration) {
        return OpenApiMerger.addDefinitions(
            consolidatedSpec,
            specToMerge,
            new com.coreoz.openapi.config.OpenApiMergerConfiguration<>(
                configuration
                    .routes()
                    .stream()
                    .map(baseRoute -> new OpenApiHttpGatewayRoute(
                        baseRoute,
                        HttpRoutes.serializeParsedPath(
                            // We need the upstream path to be able to compare what is exposed by the OpenAPI spec
                            baseRoute.upstreamParsedPath(),
                            patternName -> "{"+patternName+"}"
                        )
                    ))
                    .toList(),
                new OpenApiMergerConfigurationActions<>(
                    (operation, routeDefinition) -> OpenApiOperationAction.add(
                        configuration.operationIdPrefix() + routeDefinition.routeDefinition().routeId(),
                        makeDownstreamPathUsingOperationPathParamNames(
                            operation.parsedPath(),
                            routeDefinition.parsedPath(),
                            routeDefinition.routeDefinition().getBaseRoute().downstreamPath()
                        )
                    ),
                    OpenApiOnMissingOperation.noneAction(),
                    (parsedOperation) -> OpenApiOperationAction.none(),
                    (componentName) -> configuration.componentNamePrefix() + componentName
                )
            )
        );
    }

    /**
     * Create the downstream path compatible with the OpenAPI path params.<br>
     * <br>
     * The issue is if the operation path is <code>/pets/{petId}</code>
     * and if the downstream path is <code>/new-path-pets/{id}</code>. In that case,
     * the OpenAPI path param <code>petId</code> would not match.
     * <br>
     * So we need to use the upstream path as a pivot the calcule the path param positions
     * and update the downstream path params with the correct names. So in the previous example
     * <code>/new-path-pets/{petId}</code>
     * @param operationParsedPath The operation parsed path
     * @param upstreamParsedPath The upstream parsed path
     * @param downstreamPath The downstream path
     * @return The updated downstream path
     */
    static String makeDownstreamPathUsingOperationPathParamNames(
        ParsedPath operationParsedPath,
        ParsedPath upstreamParsedPath,
        String downstreamPath
        ) {
        if (!downstreamPath.contains("{")) {
            // fast return if there are no path parameters
            return downstreamPath;
        }

        Map<String, String> pathParametersMapping = generatePathParametersMapping(operationParsedPath, upstreamParsedPath);
        return generateDownstreamPathWithDefinitionParameters(downstreamPath, pathParametersMapping);
    }

    private static @NotNull String generateDownstreamPathWithDefinitionParameters(
        @NotNull String downstreamPath, @NotNull Map<String, String> pathParametersMapping
    ) {
        return HttpRoutes.serializeParsedPath(
            HttpRoutes.parsePath(downstreamPath),
            patternName -> "{" + pathParametersMapping.get(patternName) + "}"
        );
    }

    private static @NotNull Map<String, String> generatePathParametersMapping(
        @NotNull ParsedPath pathWithParametersToKeep, @NotNull ParsedPath pathWithParametersToRename
    ) {
        Map<String, String> mapping = new HashMap<>();
        for (int i = 0; i < pathWithParametersToKeep.segments().size() ; i++) {
            ParsedSegment currentSegmentToKeep = pathWithParametersToKeep.segments().get(i);
            if (currentSegmentToKeep.isPattern()) {
                mapping.put(pathWithParametersToRename.segments().get(i).name(), currentSegmentToKeep.name());
            }
        }
        return mapping;
    }
}
