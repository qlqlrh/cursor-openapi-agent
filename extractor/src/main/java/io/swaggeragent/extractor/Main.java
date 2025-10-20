package io.swaggeragent.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swaggeragent.extractor.model.EndpointsInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 명령행 인자를 받아서 Java 소스 코드에서 Spring Boot Controller의
 * API 엔드포인트 정보를 추출하고 JSON 파일로 출력
 * 
 * 사용법: 
 *   전체 스캔: java -jar extractor.jar --src=<소스경로> --out=<출력파일>
 *   선택적 스캔: java -jar extractor.jar --files <파일1> <파일2> ... --out=<출력파일>
 * 
 * @author qlqlrh
 * @version 1.0
 */
public class Main {
    
    /**
     * 명령행 인자를 파싱하여 소스 경로 또는 선택된 파일들과 출력 파일을 설정하고,
     * ControllerExtractor를 사용해 API 엔드포인트 정보를 추출한 후
     * JSON 파일로 저장
     * 
     * @param args 명령행 인자 배열
     *             --src=<소스경로>: 분석할 Java 소스 코드가 있는 디렉토리 경로 (전체 스캔)
     *             --files <파일1> <파일2> ...: 분석할 특정 파일들 (선택적 스캔)
     *             --out=<출력파일>: 추출된 정보를 저장할 JSON 파일 경로
     */
    public static void main(String[] args) {
        // 명령행 인자 개수 검증
        if (args.length < 2) {
            System.err.println("사용법:");
            System.err.println("  전체 스캔: java -jar extractor.jar --src=<소스경로> --out=<출력파일>");
            System.err.println("  선택적 스캔: java -jar extractor.jar --files <파일1> <파일2> ... --out=<출력파일>");
            System.exit(1);
        }

        String sourcePath = null;
        List<String> selectedFiles = new ArrayList<>();
        String outputFile = null;
        boolean filesMode = false;

        // 명령행 인자 파싱
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--src=")) {
                sourcePath = arg.substring(6);
            } else if (arg.equals("--files")) {
                filesMode = true;
                // --files 다음의 모든 인수를 파일 목록으로 처리
                i++;
                while (i < args.length && !args[i].startsWith("--")) {
                    selectedFiles.add(args[i]);
                    i++;
                }
                i--; // 마지막 증가를 되돌림
            } else if (arg.startsWith("--out=")) {
                outputFile = arg.substring(6);
            }
        }

        // 파라미터 검증
        if (outputFile == null) {
            System.err.println("--out 파라미터가 필요합니다");
            System.exit(1);
        }

        if (filesMode) {
            if (selectedFiles.isEmpty()) {
                System.err.println("--files 모드에서는 최소 하나의 파일이 필요합니다");
                System.exit(1);
            }
        } else {
            if (sourcePath == null) {
                System.err.println("--src 파라미터가 필요합니다 (전체 스캔 모드)");
                System.exit(1);
            }
        }

        try {
            ControllerExtractor extractor = new ControllerExtractor(new com.github.javaparser.JavaParser(), new ArrayList<>());
            EndpointsInfo data;
            
            if (filesMode) {
                // 선택적 파일 모드
                data = extractor.extractFromFiles(selectedFiles);
            } else {
                // 전체 스캔 모드
                data = extractor.extract(sourcePath);
            }
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT); // 가독성을 위한 들여쓰기 활성화
            mapper.writeValue(new File(outputFile), data);
            
            System.out.println("메타데이터 추출이 성공적으로 완료되었습니다!");
            System.out.println(data.getControllers().size() + "개 컨트롤러에서 " + data.getTotalMethods() + "개 메소드를 찾았습니다");
            // TODO: DTO 클래스 수 표시 (getDtoClasses 메서드 구현 후 활성화)
            // if (filesMode && data.getDtoClasses() != null && !data.getDtoClasses().isEmpty()) {
            //     System.out.println(data.getDtoClasses().size() + "개 DTO 클래스를 찾았습니다");
            // }
            System.out.println("결과가 저장되었습니다: " + outputFile);
            
        } catch (IOException e) {
            System.err.println("추출 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}



