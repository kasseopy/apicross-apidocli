package apicross.apidocli.processors;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddPrefixToSchemaNameProcessor {
    public void process(OpenAPI openAPI, String prefix) {
        Components components = openAPI.getComponents();

        processSchemas(prefix, components);
        processParameters(prefix, components);
        processHeaders(prefix, components);
        processRequestBodies(prefix, components);
        processResponses(prefix, components);

        processPaths(prefix, openAPI.getPaths());
    }

    private void processPaths(String prefix, Paths paths) {
        for (PathItem pathItem : paths.values()) {
            processParameters(prefix, pathItem.getParameters());
            Collection<Operation> operations = Utils.mapOperationsByHttpMethod(pathItem).values();
            for (Operation operation : operations) {
                processOperation(prefix, operation);
            }
        }
    }

    private void processOperation(String prefix, Operation operation) {
        processParameters(prefix, operation.getParameters());

        RequestBody requestBody = operation.getRequestBody();
        if (requestBody != null) {
            if (requestBody.get$ref() != null) {
                requestBody.set$ref(updateRef(prefix, requestBody.get$ref()));
            } else {
                Content content = requestBody.getContent();
                processMediaTypeContent(prefix, content);
            }
        }

        ApiResponses responses = operation.getResponses();
        if (responses != null) {
            for (ApiResponse response : responses.values()) {
                processApiResponse(prefix, response);
            }
        }
    }

    private void processParameters(String prefix, List<Parameter> operationParameters) {
        if (operationParameters != null) {
            for (Parameter parameter : operationParameters) {
                if (parameter.get$ref() != null) {
                    parameter.set$ref(updateRef(prefix, parameter.get$ref()));
                } else {
                    updateRefs(prefix, parameter.getSchema());
                }
            }
        }
    }

    private void processResponses(String prefix, Components components) {
        Map<String, ApiResponse> source = components.getResponses();
        if (source == null) {
            return;
        }

        Map<String, ApiResponse> outcome = new HashMap<>();
        for (Map.Entry<String, ApiResponse> entry : source.entrySet()) {
            String schemaName = entry.getKey();
            ApiResponse response = entry.getValue();
            processApiResponse(prefix, response);
            outcome.put(prefix + StringUtils.capitalize(schemaName), response);
        }
        components.setResponses(outcome);
    }

    private void processApiResponse(String prefix, ApiResponse response) {
        if (response.get$ref() != null) {
            response.set$ref(updateRef(prefix, response.get$ref()));
        } else {
            Content content = response.getContent();
            processMediaTypeContent(prefix, content);
            Map<String, Header> headers = response.getHeaders();
            if (headers != null) {
                for (Header header : headers.values()) {
                    if (header.get$ref() != null) {
                        header.set$ref(updateRef(prefix, header.get$ref()));
                    }
                }
            }
        }
    }

    private void processRequestBodies(String prefix, Components components) {
        Map<String, RequestBody> source = components.getRequestBodies();
        if (source == null) {
            return;
        }

        Map<String, RequestBody> outcome = new HashMap<>();
        for (Map.Entry<String, RequestBody> entry : source.entrySet()) {
            String schemaName = entry.getKey();
            RequestBody requestBody = entry.getValue();
            Content content = requestBody.getContent();
            processMediaTypeContent(prefix, content);
            outcome.put(prefix + StringUtils.capitalize(schemaName), requestBody);
        }
        components.setRequestBodies(outcome);
    }

    private void processMediaTypeContent(String prefix, Content content) {
        if (content == null) {
            return;
        }
        for (String mediaType : content.keySet()) {
            Schema mediaTypeSchema = content.get(mediaType).getSchema();
            updateRefs(prefix, mediaTypeSchema);
        }
    }

    private void processParameters(String prefix, Components components) {
        Map<String, Parameter> source = components.getParameters();
        if (source == null) {
            return;
        }

        Map<String, Parameter> outcome = new HashMap<>();
        for (Map.Entry<String, Parameter> entry : source.entrySet()) {
            String schemaName = entry.getKey();
            Parameter parameter = entry.getValue();
            Schema<?> schema = parameter.getSchema();
            updateRefs(prefix, schema);
            outcome.put(prefix + StringUtils.capitalize(schemaName), parameter);
        }
        components.setParameters(outcome);
    }

    private void processHeaders(String prefix, Components components) {
        Map<String, Header> source = components.getHeaders();
        if (source == null) {
            return;
        }

        Map<String, Header> outcome = new HashMap<>();
        for (Map.Entry<String, Header> entry : source.entrySet()) {
            String schemaName = entry.getKey();
            Header header = entry.getValue();
            Schema<?> schema = header.getSchema();
            updateRefs(prefix, schema);
            outcome.put(prefix + StringUtils.capitalize(schemaName), header);
        }
        components.setHeaders(outcome);
    }

    private void processSchemas(String prefix, Components components) {
        Map<String, Schema> source = components.getSchemas();
        if (source == null) {
            return;
        }

        Map<String, Schema> outcome = new HashMap<>();
        for (Map.Entry<String, Schema> entry : source.entrySet()) {
            String schemaName = entry.getKey();
            Schema<?> schema = entry.getValue();
            updateRefs(prefix, schema);
            outcome.put(prefix + StringUtils.capitalize(schemaName), schema);
        }
        components.setSchemas(outcome);
    }

    private void updateRefs(String prefix, Schema<?> schema) {
        if (schema == null) {
            return;
        }

        String $ref = schema.get$ref();

        if ($ref != null) {
            if ($ref.startsWith("#")) {
                schema.set$ref(updateRef(prefix, $ref));
            }
        } else {
            Map<String, Schema> properties = schema.getProperties();
            if (properties != null) {
                updateRefs(prefix, properties.values());
            }

            if (schema.getAdditionalProperties() instanceof Schema) {
                updateRefs(prefix, (Schema<?>) schema.getAdditionalProperties());
            }

            if (schema instanceof ComposedSchema) {
                updateRefs(prefix, ((ComposedSchema) schema).getAllOf());
                updateRefs(prefix, ((ComposedSchema) schema).getOneOf());
                updateRefs(prefix, ((ComposedSchema) schema).getAnyOf());
            }

            if (schema instanceof ArraySchema) {
                updateRefs(prefix, ((ArraySchema) schema).getItems());
            }
        }
    }

    public void updateRefs(String prefix, Collection<Schema> schemas) {
        if (schemas != null) {
            for (Schema<?> schema : schemas) {
                updateRefs(prefix, schema);
            }
        }
    }

    private String updateRef(String prefix, String $ref) {
        int index = $ref.lastIndexOf("/");
        String name = $ref.substring(index + 1);
        return $ref.substring(0, index) + "/" + prefix + StringUtils.capitalize(name);
    }
}
