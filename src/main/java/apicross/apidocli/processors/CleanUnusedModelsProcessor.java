package apicross.apidocli.processors;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.*;
import java.util.stream.Collectors;

public class CleanUnusedModelsProcessor {
    public void process(OpenAPI openAPI) {
        Set<String> allUsedRefs = indexAllUsedRefs(openAPI);

        Components components = openAPI.getComponents();

        cleanup(components.getSchemas(), refsForPrefix("#/components/schemas", allUsedRefs));
        cleanup(components.getParameters(), refsForPrefix("#/components/parameters", allUsedRefs));
        cleanup(components.getHeaders(), refsForPrefix("#/components/headers", allUsedRefs));
        cleanup(components.getResponses(), refsForPrefix("#/components/responses", allUsedRefs));
        cleanup(components.getRequestBodies(), refsForPrefix("#/components/requestBodies", allUsedRefs));
    }

    private Set<String> refsForPrefix(String prefix, Set<String> source) {
        return source.stream()
                .filter(s -> s.startsWith(prefix))
                .map(s -> s.replace(prefix, "")).collect(Collectors.toSet());
    }

    private void cleanup(Map<String, ?> source, Set<String> allUsedRefs) {
        if (source == null || source.isEmpty()) {
            return;
        }
        Set<String> keys = new HashSet<>(source.keySet());
        keys.removeAll(allUsedRefs);
        for (String unusedKey : keys) {
            source.remove(unusedKey);
        }
    }

    private Set<String> indexAllUsedRefs(OpenAPI openAPI) {
        Set<String> outcome = new HashSet<>();
        for (PathItem pathItem : openAPI.getPaths().values()) {
            List<Parameter> parameters = pathItem.getParameters();
            collectFromParameters(parameters, outcome);
            Collection<Operation> operations = Utils.mapOperationsByHttpMethod(pathItem).values();
            collectFromOperations(operations, outcome);
        }
        Components components = openAPI.getComponents();
        Collection<Schema> schemas = components.getSchemas().values();
        collectFromSchemas(schemas, outcome);
        return outcome;
    }

    private void collectFromSchemas(Collection<Schema> schemas, Set<String> outcome) {
        if (schemas == null) {
            return;
        }

        for (Schema<?> schema : schemas) {
            collectFromSchema(schema, outcome);
        }
    }

    private void collectFromSchema(Schema<?> schema, Set<String> outcome) {
        if (schema.get$ref() != null) {
            outcome.add(schema.get$ref());
        } else {
            Map<String, Schema> properties = schema.getProperties();
            if (properties != null) {
                collectFromSchemas(properties.values(), outcome);
            }

            if (schema.getAdditionalProperties() instanceof Schema) {
                collectFromSchema((Schema<?>) schema.getAdditionalProperties(), outcome);
            }

            if (schema instanceof ComposedSchema) {
                collectFromSchemas(((ComposedSchema) schema).getAllOf(), outcome);
                collectFromSchemas(((ComposedSchema) schema).getOneOf(), outcome);
                collectFromSchemas(((ComposedSchema) schema).getAnyOf(), outcome);
            }

            if (schema instanceof ArraySchema) {
                collectFromSchema(((ArraySchema) schema).getItems(), outcome);
            }
        }
    }

    private void collectFromParameters(Collection<Parameter> parameters, Set<String> outcome) {
        if (parameters == null) {
            return;
        }

        for (Parameter parameter : parameters) {
            Schema<?> parameterSchema = parameter.getSchema();
            if (parameterSchema.get$ref() != null) {
                outcome.add(parameterSchema.get$ref());
            }
        }
    }

    private void collectFromOperations(Collection<Operation> operations, Set<String> outcome) {
        if (operations == null) {
            return;
        }

        for (Operation operation : operations) {
            collectFromParameters(operation.getParameters(), outcome);
            RequestBody requestBody = operation.getRequestBody();
            if (requestBody != null) {
                Content content = requestBody.getContent();
                collectFromContent(content, outcome);
            }
            ApiResponses responses = operation.getResponses();
            if (responses != null) {
                for (ApiResponse response : responses.values()) {
                    Content content = response.getContent();
                    collectFromContent(content, outcome);
                }
            }
        }
    }

    private void collectFromContent(Content content, Set<String> outcome) {
        if (content != null) {
            Collection<MediaType> mediaTypes = content.values();
            for (MediaType mediaType : mediaTypes) {
                Schema<?> schema = mediaType.getSchema();
                if (schema != null) {
                    if (schema.get$ref() != null) {
                        outcome.add(schema.get$ref());
                    }
                }
            }
        }
    }
}
