package com.coreoz.http.openapi.merge;

import com.coreoz.http.router.SearchRouteIndexer;
import com.coreoz.http.router.data.HttpEndpoint;
import com.coreoz.http.router.data.ParsedSegment;
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
        Map<List<ParsedSegment>, List<HttpEndpoint>> indexedEndpoints = indexEndpointsToAdd(mergeConfiguration.endpoints());
        for (Map.Entry<String, PathItem> path : definitionsToBeAdded.getPaths().entrySet()) {
            List<ParsedSegment> openApiParsedEndpoint = parsedGenericEndpoint(path.getKey());
            List<HttpEndpoint> availableOperations = indexedEndpoints.get(openApiParsedEndpoint);
            if (availableOperations != null) {
                addedSchema.addAll(addPathDefinitions(
                    baseDefinitions,
                    availableOperations,
                    path.getKey(),
                    path.getValue().readOperationsMap(),
                    mergeConfiguration.operationIdPrefix(),
                    mergeConfiguration.componentNamePrefix()
                ));
            }
        }
        return addedSchema;
    }

    private static Map<List<ParsedSegment>, List<HttpEndpoint>> indexEndpointsToAdd(List<HttpEndpoint> endpoints) {
        return endpoints
            .stream()
            .collect(Collectors.groupingBy(
                endpoint -> parsedGenericEndpoint(endpoint.getUpstreamPath())
            ));
    }

    private static List<ParsedSegment> parsedGenericEndpoint(String endpoint) {
        return SearchRouteIndexer
            .parseEndpoint(endpoint)
            .stream()
            .map(segment -> new ParsedSegment(
                segment.isPattern() ? null : segment.getName(),
                segment.isPattern()
            ))
            .toList();
    }

    private static Set<OpenApiSchemaMapping> addPathDefinitions(
        OpenAPI baseDefinitions,
        List<HttpEndpoint> availableOperations,
        String definitionPath,
        Map<PathItem.HttpMethod, Operation> pathOperations,
        String operationIdPrefix,
        String componentNamePrefix
    ) {
        PathItem pathItem = new PathItem();
        Set<OpenApiSchemaMapping> addedSchema = new HashSet<>();
        for (HttpEndpoint availableOperation : availableOperations) {
            PathItem.HttpMethod operationMethod = PathItem.HttpMethod.valueOf(availableOperation.getMethod().toUpperCase());
            Operation operationDefinition = pathOperations.get(operationMethod);
            if (operationDefinition != null) {
                addedSchema.addAll(updateOperationSchemaNames(
                    updateOperationId(operationDefinition, availableOperation.getRouteId(), operationIdPrefix),
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

    private static String rewritePath(HttpEndpoint httpEndpoint, String definitionPath) {
        if (!definitionPath.contains("{")) {
            // fast return if there are no path parameters
            return definitionPath;
        }
        Map<String, String> pathParametersMapping = generatePathParametersMapping(definitionPath, httpEndpoint.getUpstreamPath());
        return generateDownstreamPathWithDefinitionParameters(httpEndpoint.getDownstreamPath(), pathParametersMapping);
    }

    private static String generateDownstreamPathWithDefinitionParameters(String downstreamPath, Map<String, String> pathParametersMapping) {
        List<ParsedSegment> downstreamPathSegments = SearchRouteIndexer.parseEndpoint(downstreamPath);
        return "/" + downstreamPathSegments
            .stream()
            .map(segment -> segment.isPattern() ?
                    "{" + pathParametersMapping.get(segment.getName()) + "}"
                    : segment.getName()
            )
            .collect(Collectors.joining("/"));
    }

    private static Map<String, String> generatePathParametersMapping(String pathWithParametersToKeep, String pathWithParametersToRename) {
        List<ParsedSegment> parsedPathToKeep = SearchRouteIndexer.parseEndpoint(pathWithParametersToKeep);
        List<ParsedSegment> parsedPathToRename = SearchRouteIndexer.parseEndpoint(pathWithParametersToRename);
        Map<String, String> mapping = new HashMap<>();
        for (int i = 0; i < parsedPathToKeep.size() ; i++) {
            ParsedSegment currentSegmentToKeep = parsedPathToKeep.get(i);
            if (currentSegmentToKeep.isPattern()) {
                mapping.put(parsedPathToRename.get(i).getName(), currentSegmentToKeep.getName());
            }
        }
        return mapping;
    }
}
