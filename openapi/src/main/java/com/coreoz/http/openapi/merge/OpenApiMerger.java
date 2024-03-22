package com.coreoz.http.openapi.merge;

import com.coreoz.http.router.data.HttpEndpoint;
import com.coreoz.http.router.data.ParsedSegment;
import com.coreoz.http.router.routes.HttpRoutes;
import com.coreoz.http.router.routes.HttpRoutesValidator;
import com.coreoz.http.router.routes.ParsedPath;
import com.coreoz.http.router.routes.ParsedRoute;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO put in a dedicated module
public class OpenApiMerger {
    private static final String COMPONENT_SCHEMA_PREFIX = "#/components/schemas/";

    /**
     * @param baseDefinitions
     * @param definitionsToBeAdded The definition to be added will be modified. If this is not the desired behavior, a copy of the base definition must be performed before calling this function
     * @param mergeConfiguration
     * @return
     */
    public static @NotNull OpenAPI addDefinitions(@NotNull OpenAPI baseDefinitions, @NotNull OpenAPI definitionsToBeAdded, @NotNull OpenApiMergerConfiguration mergeConfiguration) {
        Set<OpenApiSchemaMapping> addedSchemas = mergePaths(baseDefinitions, definitionsToBeAdded, mergeConfiguration);
        // TODO add routes for missing endpoints
        addedSchemas.addAll(updateComponentReferences(
            definitionsToBeAdded,
            mergeConfiguration.componentNamePrefix(),
            addedSchemas
                .stream()
                .map(OpenApiSchemaMapping::initialName)
                .collect(Collectors.toSet())
        ));
        mergeSchemas(baseDefinitions, definitionsToBeAdded, addedSchemas);
        return baseDefinitions;
    }

    private static void mergeSchemas(@NotNull OpenAPI baseDefinitions, @NotNull OpenAPI definitionsToBeAdded, @NotNull Set<OpenApiSchemaMapping> addedSchemas) {
        if (baseDefinitions.getComponents() == null && !addedSchemas.isEmpty()) {
            Components components = new Components();
            baseDefinitions.setComponents(components);
        }
        if (baseDefinitions.getComponents().getSchemas() == null) {
            baseDefinitions.getComponents().setSchemas(new HashMap<>());
        }
        for (OpenApiSchemaMapping schemaToBeMerged : addedSchemas) {
            baseDefinitions.getComponents().getSchemas().put(
                schemaToBeMerged.newName(),
                definitionsToBeAdded.getComponents().getSchemas().get(schemaToBeMerged.initialName())
            );
        }
    }

    @VisibleForTesting
    static @NotNull Set<OpenApiSchemaMapping> mergePaths(
        @NotNull OpenAPI baseDefinitions, @NotNull OpenAPI definitionsToBeAdded, @NotNull OpenApiMergerConfiguration mergeConfiguration
    ) {
        // TODO il faudrait indexer les endpoints du fichier de base pour gérer les conflits : replace, ignore, error
        Set<OpenApiSchemaMapping> addedSchema = new HashSet<>();
        HttpRoutesValidator<HttpEndpoint> indexedEndpoints = indexEndpointsToAdd(mergeConfiguration.endpoints());
        for (Map.Entry<String, PathItem> path : definitionsToBeAdded.getPaths().entrySet()) {
            ParsedPath definitionPath = HttpRoutes.parsePath(path.getKey());
            List<ParsedRoute<HttpEndpoint>> availableOperations = indexedEndpoints.findRoutes(definitionPath);
            if (!availableOperations.isEmpty()) {
                addedSchema.addAll(addPathDefinitions(
                    baseDefinitions,
                    availableOperations,
                    definitionPath,
                    path.getValue().readOperationsMap(),
                    mergeConfiguration.operationIdPrefix(),
                    mergeConfiguration.componentNamePrefix()
                ));
            }
        }
        return addedSchema;
    }

    @VisibleForTesting
    static @NotNull HttpRoutesValidator<HttpEndpoint> indexEndpointsToAdd(@NotNull List<HttpEndpoint> endpoints) {
        return endpoints
            .stream()
            .collect(HttpRoutesValidator.collector(
                endpoint -> HttpRoutes.parseRoute(endpoint.getUpstreamPath(), endpoint.getMethod(), endpoint)
            ));
    }

    private static @NotNull Set<OpenApiSchemaMapping> addPathDefinitions(
        @NotNull OpenAPI baseDefinitions,
        @NotNull List<ParsedRoute<HttpEndpoint>> availableOperations,
        @NotNull ParsedPath definitionPath,
        @NotNull Map<PathItem.HttpMethod, Operation> pathOperations,
        @NotNull String operationIdPrefix,
        @NotNull String componentNamePrefix
    ) {
        PathItem pathItem = new PathItem();
        Set<OpenApiSchemaMapping> addedSchema = new HashSet<>();
        for (ParsedRoute<HttpEndpoint> availableOperation : availableOperations) {
            PathItem.HttpMethod operationMethod = PathItem.HttpMethod.valueOf(availableOperation.httpMethod().toUpperCase());
            Operation operationDefinition = pathOperations.get(operationMethod);
            if (operationDefinition != null) {
                addedSchema.addAll(updateOperationSchemaNames(
                    updateOperationId(operationDefinition, availableOperation.attachedData().getRouteId(), operationIdPrefix),
                    componentNamePrefix
                ));
                pathItem.operation(
                    operationMethod,
                    operationDefinition
                );
            }
        }
        baseDefinitions.path(rewritePath(availableOperations.get(0), definitionPath), pathItem);
        return addedSchema;
    }

    private static @NotNull Operation updateOperationId(@NotNull Operation operation, @Nullable String routeId, @NotNull String operationIdPrefix) {
        if (routeId != null) {
            return operation.operationId(routeId);
        }
        return operation.operationId(Strings.nullToEmpty(operationIdPrefix) + operation.getOperationId());
    }

    private static @NotNull Set<OpenApiSchemaMapping> updateOperationSchemaNames(@NotNull Operation operation, @NotNull String componentNamePrefix) {
        Set<OpenApiSchemaMapping> schemaMappings = new HashSet<>();
        schemaMappings.addAll(updateSchemaName(componentNamePrefix, MoreObjects.firstNonNull(operation.getParameters(), List.of()), Parameter::get$ref, Parameter::set$ref));
        schemaMappings.addAll(updateSchemaName(
            componentNamePrefix,
            operation.getRequestBody() == null ? List.of() : List.of(operation.getRequestBody()),
            RequestBody::get$ref,
            RequestBody::set$ref
        ));
        schemaMappings.addAll(updateSchemaName(
            componentNamePrefix,
            operation.getResponses().entrySet(),
            entry -> entry.getValue().get$ref(),
            (entry, newSchemaName) -> entry.getValue().set$ref(newSchemaName)
        ));
        if (operation.getResponses() == null) {
            return schemaMappings;
        }
        for (var apiResponse : operation.getResponses().values()) {
            if (apiResponse.get$ref() != null) {
                String newSchemaName = renameSchema(apiResponse.get$ref(), componentNamePrefix);
                schemaMappings.add(createMapping(apiResponse.get$ref(), newSchemaName));
                apiResponse.set$ref(newSchemaName);
            }
            if (apiResponse.getContent() != null) {
                for (var contentValue : apiResponse.getContent().values()) {
                    schemaMappings.addAll(updateComponentReferences(contentValue.getSchema(), componentNamePrefix).toList());
                }
            }
        }
        return schemaMappings;
    }

    private static <T> @NotNull Set<OpenApiSchemaMapping> updateSchemaName(
        @NotNull String componentNamePrefix, @NotNull Collection<T> elements, @NotNull Function<T, String> schemaNameExtractor, @NotNull BiConsumer<T, String> schemaNameUpdater
    ) {
        Set<OpenApiSchemaMapping> schemaMappings = new HashSet<>();
        for (T element : elements) {
            String currentSchemaName = schemaNameExtractor.apply(element);
            if (currentSchemaName != null) {
                String newSchemaName = renameSchema(currentSchemaName, componentNamePrefix);
                schemaNameUpdater.accept(element, newSchemaName);
                schemaMappings.add(createMapping(currentSchemaName, newSchemaName));
            }
        }
        return schemaMappings;
    }

    private static OpenApiSchemaMapping createMapping(String currentSchemaName, String newSchemaName) {
        return new OpenApiSchemaMapping(parseSchemaName(currentSchemaName), parseSchemaName(newSchemaName));
    }

    private static @NotNull String renameSchema(@NotNull String currentComponentName, @NotNull String componentNamePrefix) {
        if (!currentComponentName.startsWith(COMPONENT_SCHEMA_PREFIX)) {
            return currentComponentName;
        }
        return COMPONENT_SCHEMA_PREFIX + componentNamePrefix + parseSchemaName(currentComponentName);
    }

    private static @NotNull String parseSchemaName(@NotNull String schemaReference) {
        return schemaReference.substring(COMPONENT_SCHEMA_PREFIX.length());
    }

    private static @NotNull String rewritePath(@NotNull ParsedRoute<HttpEndpoint> httpEndpoint, @NotNull ParsedPath definitionPath) {
        if (!definitionPath.originalPath().contains("{")) {
            // fast return if there are no path parameters
            return definitionPath.originalPath();
        }
        Map<String, String> pathParametersMapping = generatePathParametersMapping(definitionPath, httpEndpoint.parsedPath());
        return generateDownstreamPathWithDefinitionParameters(httpEndpoint.attachedData().getDownstreamPath(), pathParametersMapping);
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
                mapping.put(pathWithParametersToRename.segments().get(i).getName(), currentSegmentToKeep.getName());
            }
        }
        return mapping;
    }

    private static Set<OpenApiSchemaMapping> updateComponentReferences(
        OpenAPI definitionsToBeAdded, String componentNamePrefix, Set<String> componentToAdd
    ) {
        return definitionsToBeAdded
            .getComponents()
            .getSchemas()
            .entrySet()
            .stream()
            .filter(schema -> componentToAdd.contains(schema.getKey()))
            .map(Map.Entry::getValue)
            .flatMap(schema -> updateComponentReferences(schema, componentNamePrefix))
            .collect(Collectors.toSet());
    }

    private static Stream<OpenApiSchemaMapping> updateComponentReferences(Schema<?> schemaToBeAdded, String componentNamePrefix) {
        if (schemaToBeAdded == null) {
            return Stream.of();
        }
        return Stream.concat(
            Stream.concat(
                updateSchemaName(
                    componentNamePrefix,
                    List.of(schemaToBeAdded),
                    Schema::get$ref,
                    Schema::set$ref
                )
                    .stream(),
                schemaToBeAdded.getItems() == null ?
                    Stream.of()
                    :
                    updateSchemaName(
                        componentNamePrefix,
                        List.of(schemaToBeAdded.getItems()),
                        Schema::get$ref,
                        Schema::set$ref
                    )
                        .stream()
            ),
            MoreObjects.firstNonNull(schemaToBeAdded.getProperties(), Map.<String, Schema<?>> of())
                .values()
                .stream()
                .flatMap(schema -> updateComponentReferences(schema, componentNamePrefix))
        );
    }
}
