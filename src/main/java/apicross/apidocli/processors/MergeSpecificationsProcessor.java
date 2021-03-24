package apicross.apidocli.processors;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.*;

public class MergeSpecificationsProcessor {
    public void process(OpenAPI target, List<OpenAPI> parts) {
        if (target.getTags() == null) {
            target.setTags(new ArrayList<>());
        }

        Components targetComponents = target.getComponents();
        if (targetComponents == null) {
            targetComponents = new Components();
            target.setComponents(targetComponents);
        }

        Map<String, Schema> targetSchemas = targetComponents.getSchemas();
        if (targetSchemas == null) {
            targetSchemas = new HashMap<>();
            targetComponents.setSchemas(targetSchemas);
        }

        Map<String, Parameter> targetParameters = targetComponents.getParameters();
        if (targetParameters == null) {
            targetParameters = new HashMap<>();
            targetComponents.setParameters(targetParameters);
        }

        Map<String, Header> targetHeaders = targetComponents.getHeaders();
        if (targetHeaders == null) {
            targetHeaders = new HashMap<>();
            targetComponents.setHeaders(targetHeaders);
        }

        Map<String, RequestBody> targetRequestBodies = targetComponents.getRequestBodies();
        if (targetRequestBodies == null) {
            targetRequestBodies = new HashMap<>();
            targetComponents.setRequestBodies(targetRequestBodies);
        }

        Map<String, ApiResponse> targetResponses = targetComponents.getResponses();
        if (targetResponses == null) {
            targetResponses = new HashMap<>();
            targetComponents.setResponses(targetResponses);
        }

        Map<String, Example> targetExamples = targetComponents.getExamples();
        if (targetExamples == null) {
            targetExamples = new HashMap<>();
            targetComponents.setExamples(targetExamples);
        }

        Map<String, Object> targetExtensions = target.getExtensions();
        if (targetExtensions == null) {
            targetExtensions = new HashMap<>();
            target.setExtensions(targetExtensions);
        }

        Paths targetPaths = target.getPaths();
        if (targetPaths == null) {
            targetPaths = new Paths();
            target.setPaths(targetPaths);
        }

        for (OpenAPI part : parts) {
            target.getTags().addAll(part.getTags());

            Components partComponents = part.getComponents();

            if (partComponents.getSchemas() != null) {
                targetSchemas.putAll(partComponents.getSchemas());
            }

            if (partComponents.getParameters() != null) {
                targetParameters.putAll(partComponents.getParameters());
            }

            if (partComponents.getHeaders() != null) {
                targetHeaders.putAll(partComponents.getHeaders());
            }

            if (partComponents.getRequestBodies() != null) {
                targetRequestBodies.putAll(partComponents.getRequestBodies());
            }

            if (partComponents.getResponses() != null) {
                targetResponses.putAll(partComponents.getResponses());
            }

            if (partComponents.getExamples() != null) {
                targetExamples.putAll(partComponents.getExamples());
            }

            if (part.getExtensions() != null) {
                targetExtensions.putAll(part.getExtensions());
            }

            if (part.getPaths() != null) {
                targetPaths.putAll(part.getPaths());
            }
        }
    }
}
