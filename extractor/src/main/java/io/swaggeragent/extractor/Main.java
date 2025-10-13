package io.swaggeragent.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swaggeragent.extractor.model.EndpointsData;

import java.io.File;
import java.io.IOException;

/**
 * 명령행 인자를 받아서 Java 소스 코드에서 Spring Boot Controller의
 * API 엔드포인트 정보를 추출하고 JSON 파일로 출력
 * 
 * 사용법: java -jar extractor.jar --src=<소스경로> --out=<출력파일>
 * 
 * @author qlqlrh
 * @version 1.0
 */
public class Main {
    
    /**
     * 명령행 인자를 파싱하여 소스 경로와 출력 파일을 설정하고,
     * ControllerExtractor를 사용해 API 엔드포인트 정보를 추출한 후
     * JSON 파일로 저장
     * 
     * @param args 명령행 인자 배열
     *             --src=<소스경로>: 분석할 Java 소스 코드가 있는 디렉토리 경로
     *             --out=<출력파일>: 추출된 정보를 저장할 JSON 파일 경로
     */
    public static void main(String[] args) {
        // 명령행 인자 개수 검증
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
            mapper.enable(SerializationFeature.INDENT_OUTPUT); // 가독성을 위한 들여쓰기 활성화
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



