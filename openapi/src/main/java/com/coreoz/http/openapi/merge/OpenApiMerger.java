package com.coreoz.http.openapi.merge;

import com.coreoz.http.router.routes.HttpRoutes;
import com.coreoz.http.router.routes.HttpRoutesValidator;
import com.coreoz.http.router.data.HttpEndpoint;
import com.coreoz.http.router.data.ParsedSegment;
import com.coreoz.http.router.routes.ParsedPath;
import com.coreoz.http.router.routes.ParsedRoute;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
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
    public static OpenAPI addDefinitions(OpenAPI baseDefinitions, OpenAPI definitionsToBeAdded, OpenApiMergerConfiguration mergeConfiguration) {
        Set<OpenApiSchemaMapping> addedSchemas = mergePaths(baseDefinitions, definitionsToBeAdded, mergeConfiguration);
        // TODO add routes for missing endpoints
        mergeSchemas(baseDefinitions, definitionsToBeAdded, addedSchemas);
        return baseDefinitions;
    }

    private static void mergeSchemas(OpenAPI baseDefinitions, OpenAPI definitionsToBeAdded, Set<OpenApiSchemaMapping> addedSchemas) {
        if (baseDefinitions.getComponents() == null && !addedSchemas.isEmpty()) {
            Components components = new Components();
            components.setSchemas(new HashMap<>());
            baseDefinitions.setComponents(components);
        }
        for (OpenApiSchemaMapping schemaToBeMerged : addedSchemas) {
            baseDefinitions.getComponents().getSchemas().put(
                schemaToBeMerged.newName(),
                definitionsToBeAdded.getComponents().getSchemas().get(schemaToBeMerged.initialName())
            );
        }
    }

    private static Set<OpenApiSchemaMapping> mergePaths(OpenAPI baseDefinitions, OpenAPI definitionsToBeAdded, OpenApiMergerConfiguration mergeConfiguration) {
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

    private static HttpRoutesValidator<HttpEndpoint> indexEndpointsToAdd(List<HttpEndpoint> endpoints) {
        return endpoints
            .stream()
            .collect(HttpRoutesValidator.collector(
                endpoint -> HttpRoutes.parseRoute(endpoint.getUpstreamPath(), endpoint.getMethod(), endpoint)
            ));
    }

    private static Set<OpenApiSchemaMapping> addPathDefinitions(
        OpenAPI baseDefinitions,
        List<ParsedRoute<HttpEndpoint>> availableOperations,
        ParsedPath definitionPath,
        Map<PathItem.HttpMethod, Operation> pathOperations,
        String operationIdPrefix,
        String componentNamePrefix
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

    private static Operation updateOperationId(Operation operation, String routeId, String operationIdPrefix) {
        if (routeId != null) {
            return operation.operationId(routeId);
        }
        return operation.operationId(Strings.nullToEmpty(operationIdPrefix) + operation.getOperationId());
    }

    private static Set<OpenApiSchemaMapping> updateOperationSchemaNames(Operation operation, String componentNamePrefix) {
        Set<OpenApiSchemaMapping> schemaMappings = new HashSet<>();
        schemaMappings.addAll(updateSchemaName(componentNamePrefix, MoreObjects.firstNonNull(operation.getParameters(), List.of()), Parameter::get$ref, Parameter::set$ref));
        schemaMappings.addAll(updateSchemaName(
            componentNamePrefix,
            operation.getRequestBody() == null ? List.of() : List.of(operation.getRequestBody()),
            RequestBody::get$ref,
            RequestBody::set$ref)
        );
        schemaMappings.addAll(updateSchemaName(
            componentNamePrefix,
            operation.getResponses().entrySet(),
            entry -> entry.getValue().get$ref(),
            (entry, newSchemaName) -> entry.getValue().set$ref(newSchemaName)
        ));
        schemaMappings.addAll(updateSchemaName(
            componentNamePrefix,
            operation
                .getResponses()
                .entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().getContent() == null ?
                    Stream.of()
                    : entry.getValue().getContent().entrySet().stream()
                )
                .toList(),
            entry -> entry.getValue().getSchema().get$ref(),
            (entry, newSchemaName) -> entry.getValue().getSchema().set$ref(newSchemaName)
        ));
        return schemaMappings;
    }

    private static <T> Set<OpenApiSchemaMapping> updateSchemaName(
        String componentNamePrefix, Collection<T> elements, Function<T, String> schemaNameExtractor, BiConsumer<T, String> schemaNameUpdater
    ) {
        Set<OpenApiSchemaMapping> schemaMappings = new HashSet<>();
        for (T element : elements) {
            String currentSchemaName = schemaNameExtractor.apply(element);
            if (currentSchemaName != null) {
                String newSchemaName = renameSchema(currentSchemaName, componentNamePrefix);
                schemaNameUpdater.accept(element, newSchemaName);
                schemaMappings.add(new OpenApiSchemaMapping(
                    parseSchemaName(currentSchemaName),
                    parseSchemaName(newSchemaName)
                ));
            }
        }
        return schemaMappings;
    }

    private static String renameSchema(String currentComponentName, String componentNamePrefix) {
        if (!currentComponentName.startsWith(COMPONENT_SCHEMA_PREFIX)) {
            return currentComponentName;
        }
        return COMPONENT_SCHEMA_PREFIX + componentNamePrefix + parseSchemaName(currentComponentName);
    }

    private static String parseSchemaName(String schemaReference) {
        return schemaReference.substring(COMPONENT_SCHEMA_PREFIX.length());
    }

    private static String rewritePath(ParsedRoute<HttpEndpoint> httpEndpoint, ParsedPath definitionPath) {
        if (!definitionPath.originalPath().contains("{")) {
            // fast return if there are no path parameters
            return definitionPath.originalPath();
        }
        Map<String, String> pathParametersMapping = generatePathParametersMapping(definitionPath, httpEndpoint.parsedPath());
        return generateDownstreamPathWithDefinitionParameters(httpEndpoint.attachedData().getDownstreamPath(), pathParametersMapping);
    }

    private static String generateDownstreamPathWithDefinitionParameters(String downstreamPath, Map<String, String> pathParametersMapping) {
        return HttpRoutes.serializeParsedPath(
            HttpRoutes.parsePath(downstreamPath),
            patternName -> "{" + pathParametersMapping.get(patternName) + "}"
        );
    }

    private static Map<String, String> generatePathParametersMapping(
        ParsedPath pathWithParametersToKeep, ParsedPath pathWithParametersToRename
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
}
