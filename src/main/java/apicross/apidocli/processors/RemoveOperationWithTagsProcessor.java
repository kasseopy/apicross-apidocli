package apicross.apidocli.processors;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RemoveOperationWithTagsProcessor {
    public void process(OpenAPI openAPI, Set<String> tagsToCutOff) {
        Set<String> uriPathsWithoutOperations = new HashSet<>();
        Set<String> tagsWithOperations = new HashSet<>();

        Paths paths = openAPI.getPaths();
        for (String uriPath : paths.keySet()) {
            PathItem pathItem = paths.get(uriPath);
            if (needsToBeIgnored(pathItem.getGet(), tagsToCutOff)) {
                pathItem.setGet(null);
            }
            if (needsToBeIgnored(pathItem.getPost(), tagsToCutOff)) {
                pathItem.setPost(null);
            }
            if (needsToBeIgnored(pathItem.getPut(), tagsToCutOff)) {
                pathItem.setPut(null);
            }
            if (needsToBeIgnored(pathItem.getDelete(), tagsToCutOff)) {
                pathItem.setDelete(null);
            }
            if (needsToBeIgnored(pathItem.getPatch(), tagsToCutOff)) {
                pathItem.setPatch(null);
            }
            if (needsToBeIgnored(pathItem.getHead(), tagsToCutOff)) {
                pathItem.setHead(null);
            }
            if (needsToBeIgnored(pathItem.getOptions(), tagsToCutOff)) {
                pathItem.setOptions(null);
            }
            if (needsToBeIgnored(pathItem.getTrace(), tagsToCutOff)) {
                pathItem.setTrace(null);
            }

            Map<String, Operation> operationMap = Utils.mapOperationsByHttpMethod(pathItem);
            if (operationMap.isEmpty()) {
                uriPathsWithoutOperations.add(uriPath);
            } else {
                for (Operation operation : operationMap.values()) {
                    List<String> tags = operation.getTags();
                    if (!tags.isEmpty()) {
                        tagsWithOperations.addAll(tags);
                    }
                }
            }
        }

        for (String uriPathWithNoOperations : uriPathsWithoutOperations) {
            openAPI.getPaths().remove(uriPathWithNoOperations);
        }

        openAPI.getTags().removeIf(tag -> !tagsWithOperations.contains(tag.getName()));
    }

    private boolean needsToBeIgnored(Operation operation, Set<String> tagsToCutOff) {
        if (operation == null) {
            return false;
        }
        List<String> tags = operation.getTags();
        if (tags == null) {
            return false;
        }
        for (String tag : tags) {
            if (tagsToCutOff.contains(tag)) {
                return true;
            }
        }
        return false;
    }

}
