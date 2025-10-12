package io.swaggeragent.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swaggeragent.extractor.model.EndpointsData;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java -jar extractor.jar --src=<source_path> --out=<output_file>");
            System.exit(1);
        }

        String sourcePath = null;
        String outputFile = null;

        for (String arg : args) {
            if (arg.startsWith("--src=")) {
                sourcePath = arg.substring(6);
            } else if (arg.startsWith("--out=")) {
                outputFile = arg.substring(6);
            }
        }

        if (sourcePath == null || outputFile == null) {
            System.err.println("Both --src and --out parameters are required");
            System.exit(1);
        }

        try {
            ControllerExtractor extractor = new ControllerExtractor();
            EndpointsData data = extractor.extract(sourcePath);
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(new File(outputFile), data);
            
            System.out.println("Extraction completed successfully!");
            System.out.println("Found " + data.getTotalMethods() + " methods in " + data.getControllers().size() + " controllers");
            System.out.println("Output written to: " + outputFile);
            
        } catch (IOException e) {
            System.err.println("Error during extraction: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}



