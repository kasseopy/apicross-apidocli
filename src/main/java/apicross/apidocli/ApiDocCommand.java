package apicross.apidocli;

import apicross.apidocli.processors.AddPrefixToSchemaNameProcessor;
import apicross.apidocli.processors.RemoveOperationWithTagsProcessor;
import apicross.apidocli.processors.MergeSpecificationsProcessor;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.base.Preconditions;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "apidoc", description = "Prepare single API specification for publication from multiple sources")
public class ApiDocCommand implements Callable<Integer> {
    @CommandLine.Option(names = "-dir", required = true, description = "File directory with specification files")
    String dir;
    @CommandLine.Option(names = "-s", arity = "2..*", required = true,
            description = "Specification file names. Format {specification file name}[#prefix], " +
                    "where prefix - prefix to be added to the schema name, " +
                    "to prevent model names collisions from different sources")
    String[] specifications;
    @CommandLine.Option(names = "-o", required = true, description = "Output specification file name")
    String outputFileName;
    @CommandLine.Option(names = "-t", arity = "1..*", description = "Tags for operations to be removed")
    String[] cutOffTags;

    @Override
    public Integer call() throws Exception {
        Preconditions.checkState(specifications.length >= 2);
        OpenAPI mainSpecification = read(specLocation(specifications[0]), false);

        List<OpenAPI> specificationsToBeJoined = new ArrayList<>();

        for (int i = 1; i < specifications.length; i++) {
            OpenAPI particularSpecification = processParticularSpecification(specifications[i]);
            specificationsToBeJoined.add(particularSpecification);
        }

        merge(mainSpecification, specificationsToBeJoined);

        ((YAMLFactory) Yaml.mapper().getFactory()).disable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        String specification = Yaml.pretty().writeValueAsString(mainSpecification);

        File tempFile = File.createTempFile("merged-spec", "yaml", new File(dir));
        tempFile.deleteOnExit();

        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(tempFile)))) {
            writer.write(specification);
            writer.flush();
        }

        OpenAPI resolvedAPI = read(tempFile.getCanonicalPath(), true);
        specification = Yaml.pretty().writeValueAsString(resolvedAPI);

        File output = new File(outputFileName);

        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(output)))) {
            writer.write(specification);
            writer.flush();
        }

        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ApiDocCommand()).execute(args);
        System.exit(exitCode);
    }

    private OpenAPI processParticularSpecification(String specificationLocationWithNs) {
        String[] parts = specificationLocationWithNs.split("#");

        String specificationPath, prefix;

        if (parts.length == 2) {
            specificationPath = parts[0];
            prefix = parts[1];
        } else {
            specificationPath = parts[0];
            prefix = null;
        }

        OpenAPI particularSpecification = read(specLocation(specificationPath), false);

        if (prefix != null) {
            addPrefixToSchemaName(particularSpecification, prefix);
        }

        if (cutOffTags != null && cutOffTags.length > 0) {
            cutOffTags(particularSpecification, cutOffTags);
        }

        return particularSpecification;
    }

    private void addPrefixToSchemaName(OpenAPI specification, String namespace) {
        AddPrefixToSchemaNameProcessor processor = new AddPrefixToSchemaNameProcessor();
        processor.process(specification, namespace);
    }

    private void cutOffTags(OpenAPI specification, String[] cutOffTags) {
        RemoveOperationWithTagsProcessor processor = new RemoveOperationWithTagsProcessor();
        List<String> list = Arrays.asList(cutOffTags);
        processor.process(specification, new HashSet<>(list));
    }

    private void merge(OpenAPI mainSpecification, List<OpenAPI> joinedSpecifications) {
        MergeSpecificationsProcessor processor = new MergeSpecificationsProcessor();
        processor.process(mainSpecification, joinedSpecifications);
    }

    private OpenAPI read(String location, boolean resolve) {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(resolve);
        return new OpenAPIV3Parser().read(location, null, parseOptions);
    }

    private String specLocation(String fileName) {
        return new File(dir, fileName).getPath();
    }
}
