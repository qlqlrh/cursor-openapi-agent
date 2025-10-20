package io.swaggeragent.extractor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import io.swaggeragent.extractor.model.*;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Spring Boot Controller에서 API 엔드포인트 정보를 추출
 * 
 * JavaParser를 사용하여 Java 소스 코드를 AST(Abstract Syntax Tree)로 파싱하고,
 * Spring Boot의 @RestController, @Controller 어노테이션이 있는 클래스에서
 * API 엔드포인트 정보를 추출
 * 
 * @author qlqlrh
 * @version 1.0
 */
@RequiredArgsConstructor
public class ControllerExtractor {
    // Java 소스 코드 파싱을 위한 JavaParser
    private final JavaParser javaParser;
    
    // 추출된 Controller 정보를 저장하는 리스트
    private final List<ControllerInfo> controllers;

    /**
     * 지정된 소스 경로에서 Controller 정보를 추출
     */
    public EndpointsData extract(String sourcePath) throws IOException {
        Path sourceDir = Paths.get(sourcePath);
        
        // 디렉토리를 재귀적으로 탐색
        Files.walk(sourceDir)
            .filter(path -> path.toString().endsWith(".java"))  // .java 파일만 필터링
            .filter(path -> path.toString().contains("controller"))  // 'controller'가 포함된 파일만 필터링
            .forEach(this::processFile);  // 각 파일을 처리
        
        return new EndpointsData(controllers);
    }

    /**
     * 선택된 파일들에서 Controller 정보를 추출
     */
    public EndpointsData extractFromFiles(List<String> filePaths) throws IOException {
        for (String filePath : filePaths) {
            Path path = Paths.get(filePath);
            if (Files.exists(path) && path.toString().endsWith(".java")) {
                processFile(path);
            } else {
                System.err.println("파일을 찾을 수 없거나 Java 파일이 아닙니다: " + filePath);
            }
        }
        
        return new EndpointsData(controllers);
    }

    /**
     * JavaParser를 사용하여 파일을 파싱하고, ControllerVisitor를 통해
     * Controller 클래스와 메서드 정보를 추출
     */
    private void processFile(Path filePath) {
        try {
            // 파일을 AST로 파싱
            CompilationUnit cu = javaParser.parse(filePath).getResult().orElse(null);
            if (cu == null) return;

            // AST를 순회하며 Controller 정보 추출
            cu.accept(new ControllerVisitor(filePath), null);
        } catch (Exception e) {
            System.err.println("Error processing file: " + filePath + " - " + e.getMessage());
        }
    }

    /**
     * AST를 순회하며 Controller 클래스와 메서드 정보를 추출하
     */
    private class ControllerVisitor extends VoidVisitorAdapter<Void> {
        public ControllerVisitor(Path filePath) {
            // filePath는 현재 사용하지 않지만 향후 확장을 위해 유지
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            if (isController(n)) {
                ControllerInfo controller = extractController(n);
                if (controller != null) {
                    controllers.add(controller);
                }
            }
            // 하위 노드들도 방문
            super.visit(n, arg);
        }

        private boolean isController(ClassOrInterfaceDeclaration n) {
            return n.getAnnotations().stream()
                .anyMatch(ann -> ann.getNameAsString().equals("RestController") || 
                               ann.getNameAsString().equals("Controller"));
        }

        /**
         * Controller 클래스에서 정보 추출
         */
        private ControllerInfo extractController(ClassOrInterfaceDeclaration n) {
            // 기본 Controller 정보 추출
            String className = n.getNameAsString();
            String packageName = extractPackageName(n);
            String requestMapping = extractRequestMapping(n);
            
            ControllerInfo controller = new ControllerInfo(className, packageName, requestMapping);
            
            // HTTP 매핑 메서드들 추출
            List<MethodInfo> methods = n.getMethods().stream()
                .filter(this::isMappingMethod)  // HTTP 매핑 어노테이션이 있는 메서드만 필터링
                .map(this::extractMethod)       // 각 메서드에서 정보 추출
                .filter(Objects::nonNull)       // null이 아닌 메서드만 유지
                .collect(Collectors.toList());
            
            controller.setMethods(methods);
            return controller;
        }

        /**
         * 클래스 패키지명 추출
         */
        private String extractPackageName(ClassOrInterfaceDeclaration n) {
            return n.findCompilationUnit()
                .map(cu -> cu.getPackageDeclaration()
                    .map(pkg -> pkg.getNameAsString())
                    .orElse(""))
                .orElse("");
        }

        /**
         * 클래스의 @RequestMapping 어노테이션 값 추출
         */
        private String extractRequestMapping(ClassOrInterfaceDeclaration n) {
            return n.getAnnotations().stream()
                .filter(ann -> ann.getNameAsString().equals("RequestMapping"))
                .findFirst()
                .map(this::extractAnnotationValue)
                .orElse("");
        }

        /**
         * 어노테이션의 value 값 추출
         */
        private String extractAnnotationValue(AnnotationExpr annotation) {
            if (annotation.isSingleMemberAnnotationExpr()) {
                // @RequestMapping("/api/users") 형태
                return annotation.asSingleMemberAnnotationExpr()
                    .getMemberValue()
                    .asStringLiteralExpr()
                    .getValue();
            } else if (annotation.isNormalAnnotationExpr()) {
                // @RequestMapping(value="/api/users") 형태
                return annotation.asNormalAnnotationExpr()
                    .getPairs().stream()
                    .filter(pair -> pair.getNameAsString().equals("value"))
                    .findFirst()
                    .map(pair -> pair.getValue().asStringLiteralExpr().getValue())
                    .orElse("");
            }
            return "";
        }

        /**
         * 주어진 메서드가 HTTP 매핑 메서드인지 확인
         */
        private boolean isMappingMethod(MethodDeclaration n) {
            return n.getAnnotations().stream()
                .anyMatch(ann -> {
                    String name = ann.getNameAsString();
                    return name.equals("GetMapping") || name.equals("PostMapping") || 
                           name.equals("PutMapping") || name.equals("DeleteMapping") ||
                           name.equals("RequestMapping");
                });
        }

        /**
         * HTTP 매핑 메서드에서 정보 추출
         */
        private MethodInfo extractMethod(MethodDeclaration n) {
            // 기본 메서드 정보 추출
            String methodName = n.getNameAsString();
            String httpMethod = extractHttpMethod(n);
            String path = extractMethodPath(n);
            
            MethodInfo method = new MethodInfo(methodName, httpMethod, path);
            method.setLineNumber(n.getBegin().map(pos -> pos.line).orElse(0));
            method.setReturnType(extractReturnType(n));
            method.setParameters(extractParameters(n));
            method.setExceptions(extractExceptions(n));
            
            return method;
        }

        /**
         * 메서드의 HTTP 메서드 추출
         */
        private String extractHttpMethod(MethodDeclaration n) {
            return n.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .filter(name -> name.endsWith("Mapping"))
                .map(name -> name.replace("Mapping", "").toUpperCase())
                .findFirst()
                .orElse("GET");
        }

        /**
         * 메서드의 경로 추출
         */
        private String extractMethodPath(MethodDeclaration n) {
            return n.getAnnotations().stream()
                .filter(ann -> ann.getNameAsString().endsWith("Mapping"))
                .findFirst()
                .map(this::extractAnnotationValue)
                .orElse("");
        }

        private String extractReturnType(MethodDeclaration n) {
            return n.getType().toString();
        }

        /**
         * 메서드의 파라미터 목록 추출
         */
        private List<ParameterInfo> extractParameters(MethodDeclaration n) {
            return n.getParameters().stream()
                .map(param -> {
                    String name = param.getNameAsString();
                    String type = param.getType().toString();
                    String in = determineParameterIn(param);        // 파라미터 위치 결정
                    boolean required = isParameterRequired(param);  // 필수 여부 결정
                    
                    ParameterInfo paramInfo = new ParameterInfo(name, type, in, required);
                    paramInfo.setValidationAnnotations(extractValidationAnnotations(param));
                    return paramInfo;
                })
                .collect(Collectors.toList());
        }

        /**
         * 파라미터의 위치 결정
         */
        private String determineParameterIn(com.github.javaparser.ast.body.Parameter param) {
            // @RequestBody가 있으면 body로 분류
            boolean hasRequestBody = param.getAnnotations().stream()
                .anyMatch(ann -> ann.getNameAsString().equals("RequestBody"));
            
            if (hasRequestBody) {
                return "body";
            }
            
            // @PathVariable이 있으면 path로 분류
            boolean hasPathVariable = param.getAnnotations().stream()
                .anyMatch(ann -> ann.getNameAsString().equals("PathVariable"));
            
            if (hasPathVariable) {
                return "path";
            }
            
            // @RequestHeader가 있으면 header로 분류
            boolean hasRequestHeader = param.getAnnotations().stream()
                .anyMatch(ann -> ann.getNameAsString().equals("RequestHeader"));
            
            if (hasRequestHeader) {
                return "header";
            }
            
            // @RequestParam이 있거나 기본값은 query로 분류
            return "query";
        }

        private boolean isParameterRequired(com.github.javaparser.ast.body.Parameter param) {
            return param.getAnnotations().stream()
                .anyMatch(ann -> ann.getNameAsString().equals("PathVariable")) ||
               param.getAnnotations().stream()
                .anyMatch(ann -> ann.getNameAsString().equals("RequestBody"));
        }

        /**
         * 파라미터의 검증 어노테이션 추출
         */
        private String[] extractValidationAnnotations(com.github.javaparser.ast.body.Parameter param) {
            return param.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .filter(name -> name.equals("Valid") || name.equals("NotNull") || 
                              name.equals("Size") || name.equals("NotBlank") ||
                              name.equals("RequestBody") || name.equals("PathVariable") ||
                              name.equals("RequestParam") || name.equals("RequestHeader"))
                .toArray(String[]::new);
        }

        /**
         * 메서드가 던질 수 있는 예외 목록 추출합
         */
        private List<String> extractExceptions(MethodDeclaration n) {
            return n.getThrownExceptions().stream()
                .map(exception -> exception.toString())
                .collect(Collectors.toList());
        }
    }
}



