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
    
    // 추출된 DTO 정보를 저장하는 리스트
    private final List<DtoInfo> dtoClasses;
    
    // 프로젝트 루트 경로 (파일 검색용)
    private String projectRoot = System.getProperty("user.dir");

    /**
     * 지정된 소스 경로에서 Controller 정보를 추출
     */
    public EndpointsInfo extract(String sourcePath) throws IOException {
        Path sourceDir = Paths.get(sourcePath);
        
        // 디렉토리를 재귀적으로 탐색
        Files.walk(sourceDir)
            .filter(path -> path.toString().endsWith(".java"))  // .java 파일만 필터링
            .filter(path -> path.toString().contains("controller"))  // 'controller'가 포함된 파일만 필터링
            .forEach(this::processFile);  // 각 파일을 처리
        
        return EndpointsInfo.ofControllersAndDtos(controllers, dtoClasses);
    }

    /**
     * 선택된 파일들에서 Controller와 DTO 정보를 통합 추출
     * - 컨트롤러 파일: API 엔드포인트 정보 추출
     * - DTO 파일: 데이터 모델 정보 추출
     * - 혼합 파일: 컨트롤러와 DTO 모두 처리
     */
    public EndpointsInfo extractFromFiles(List<String> filePaths) throws IOException {
        int processedFiles = 0;
        int errorFiles = 0;
        int controllerFiles = 0;
        int dtoFiles = 0;
        
        for (String filePath : filePaths) {
            Path path = Paths.get(filePath);
            if (Files.exists(path) && path.toString().endsWith(".java")) {
                FileProcessResult result = processFileWithResult(path);
                if (result.isSuccess()) {
                    processedFiles++;
                    if (result.isControllerFile()) controllerFiles++;
                    if (result.isDtoFile()) dtoFiles++;
                } else {
                    errorFiles++;
                }
            } else {
                System.err.println("파일을 찾을 수 없거나 Java 파일이 아닙니다: " + filePath);
                errorFiles++;
            }
        }
        
        // 처리된 파일이 없으면 빈 결과 반환
        if (processedFiles == 0) {
            return EndpointsInfo.ofControllersAndDtos(new ArrayList<>(), new ArrayList<>());
        }
        
        return EndpointsInfo.ofControllersAndDtos(controllers, dtoClasses);
    }

    /**
     * 파일 처리 결과를 담는 클래스
     */
    private static class FileProcessResult {
        private final boolean success;
        private final boolean controllerFile;
        private final boolean dtoFile;
        private final String errorMessage;
        
        public FileProcessResult(boolean success, boolean controllerFile, boolean dtoFile, String errorMessage) {
            this.success = success;
            this.controllerFile = controllerFile;
            this.dtoFile = dtoFile;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() { return success; }
        public boolean isControllerFile() { return controllerFile; }
        public boolean isDtoFile() { return dtoFile; }
        public String getErrorMessage() { return errorMessage; }
        
        public static FileProcessResult success(boolean controllerFile, boolean dtoFile) {
            return new FileProcessResult(true, controllerFile, dtoFile, null);
        }
        
        public static FileProcessResult error(String errorMessage) {
            return new FileProcessResult(false, false, false, errorMessage);
        }
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

            // 파일명으로 DTO 클래스인지 확인
            if (isDtoFile(filePath)) {
                cu.accept(new DtoVisitor(filePath), null);
            } else {
                // AST를 순회하며 Controller 정보 추출
                cu.accept(new ControllerVisitor(filePath), null);
            }
        } catch (Exception e) {
            System.err.println("Error processing file: " + filePath + " - " + e.getMessage());
        }
    }

    /**
     * 파일 처리 결과를 반환하는 개선된 메서드
     */
    private FileProcessResult processFileWithResult(Path filePath) {
        try {
            // 파일을 AST로 파싱
            CompilationUnit cu = javaParser.parse(filePath).getResult().orElse(null);
            if (cu == null) {
                return FileProcessResult.error("파일을 파싱할 수 없습니다: " + filePath);
            }

            boolean isDto = isDtoFile(filePath);
            boolean isController = false;
            
            // 파일명으로 DTO 클래스인지 확인
            if (isDto) {
                cu.accept(new DtoVisitor(filePath), null);
            } else {
                // AST를 순회하며 Controller 정보 추출
                cu.accept(new ControllerVisitor(filePath), null);
                isController = true; // 컨트롤러 파일로 처리됨
            }
            
            return FileProcessResult.success(isController, isDto);
            
        } catch (Exception e) {
            return FileProcessResult.error("파일 처리 중 오류 발생: " + filePath + " - " + e.getMessage());
        }
    }

    /**
     * 파일명 패턴으로 DTO 클래스인지 확인
     * 패턴: *Dto, *DTO, *Req, *Res,*Request, *Response
     */
    private boolean isDtoFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        String className = fileName.substring(0, fileName.lastIndexOf('.'));
        
        return className.endsWith("Dto") || 
               className.endsWith("DTO") || 
               className.endsWith("Req") || 
               className.endsWith("Res") || 
               className.endsWith("Request") || 
               className.endsWith("Response");
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
            String requestMapping = extractRequestMapping(n);
            
            ControllerInfo controller = ControllerInfo.builder()
                .className(className)
                .requestMapping(requestMapping)
                .build();
            
            // HTTP 매핑 메서드들 추출
            List<MethodInfo> methods = n.getMethods().stream()
                .filter(this::isMappingMethod)  // HTTP 매핑 어노테이션이 있는 메서드만 필터링
                .map(this::extractMethod)       // 각 메서드에서 정보 추출
                .filter(Objects::nonNull)       // null이 아닌 메서드만 유지
                .collect(Collectors.toList());
            
            controller.setMethods(methods);
            
            // 컨트롤러에서 사용되는 DTO 클래스들 감지
            detectRelatedDtos(n);
            
            return controller;
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
            
            MethodInfo method = MethodInfo.builder()
                .methodName(methodName)
                .httpMethod(httpMethod)
                .path(path)
                .lineNumber(n.getBegin().map(pos -> pos.line).orElse(0))
                .returnType(extractReturnType(n))
                .parameters(extractParameters(n))
                .exceptions(extractExceptions(n))
                .build();
            
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
                    
                    ParameterInfo paramInfo = ParameterInfo.builder()
                        .name(name)
                        .type(type)
                        .in(in)
                        .required(required)
                        .validationAnnotations(extractValidationAnnotations(param))
                        .build();
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

        /**
         * 컨트롤러에서 사용되는 DTO 클래스들 감지
         * @RequestBody 파라미터와 반환 타입에서 DTO 클래스 추출
         */
        private void detectRelatedDtos(ClassOrInterfaceDeclaration controllerClass) {
            // 모든 메서드에서 DTO 클래스 감지
            controllerClass.getMethods().stream()
                .filter(this::isMappingMethod)
                .forEach(method -> {
                    // @RequestBody 파라미터에서 DTO 감지
                    detectDtoFromParameters(method);
                    
                    // 반환 타입에서 DTO 감지
                    detectDtoFromReturnType(method);
                });
        }

        /**
         * 메서드 파라미터에서 DTO 클래스 감지
         */
        private void detectDtoFromParameters(MethodDeclaration method) {
            method.getParameters().stream()
                .filter(param -> param.getAnnotations().stream()
                    .anyMatch(ann -> ann.getNameAsString().equals("RequestBody")))
                .forEach(param -> {
                    String typeName = param.getType().toString();
                    String className = TypeParser.parseType(typeName, TypeParser.ParseMode.CLASS_NAME_ONLY).getBaseType();
                    if (isDtoClassName(className)) {
                        addDtoIfNotExists(className, method);
                    }
                });
        }

        /**
         * 메서드 반환 타입에서 DTO 클래스 감지
         */
        private void detectDtoFromReturnType(MethodDeclaration method) {
            String returnType = method.getType().toString();
            
            // 직접 반환 타입 확인
            String className = TypeParser.parseType(returnType, TypeParser.ParseMode.CLASS_NAME_ONLY).getBaseType();
            if (isDtoClassName(className)) {
                addDtoIfNotExists(className, method);
            }
            
            extractDtosFromGenericType(returnType, method);
        }

        /**
         * 제네릭 타입에서 DTO 클래스들을 재귀적으로 추출
         * 예: List<UserDto>, ResponseEntity<UserDto>, Optional<List<UserDto>> 등
         */
        private void extractDtosFromGenericType(String typeName, MethodDeclaration method) {
            TypeParseResult result = TypeParser.parseType(typeName, TypeParser.ParseMode.DTO_EXTRACTION);
            
            for (String genericType : result.getGenericTypes()) {
                String className = TypeParser.parseType(genericType, TypeParser.ParseMode.CLASS_NAME_ONLY).getBaseType();
                if (isDtoClassName(className)) {
                    addDtoIfNotExists(className, method);
                }
                
                // 중첩된 제네릭 타입도 재귀적으로 처리
                extractDtosFromGenericType(genericType, method);
            }
        }


        /**
         * 클래스명이 DTO 패턴인지 확인
         */
        private boolean isDtoClassName(String className) {
            return className.endsWith("Dto") || 
                   className.endsWith("DTO") || 
                   className.endsWith("Req") || 
                   className.endsWith("Res") || 
                   className.endsWith("Request") || 
                   className.endsWith("Response");
        }

        /**
         * DTO 클래스가 이미 존재하지 않으면 추가
         */
        private void addDtoIfNotExists(String className, MethodDeclaration method) {
            // 이미 존재하는지 확인
            boolean exists = dtoClasses.stream()
                .anyMatch(dto -> dto.getClassName().equals(className));
            
            if (!exists) {
                // 파일 경로 찾기
                String filePath = findDtoFileByClassName(className);
                
                // 파일을 찾지 못한 경우 추정 경로 사용
                if (filePath == null || !Files.exists(Paths.get(filePath))) {
                    filePath = "src/main/java/com/example/dto/" + className + ".java";
                }
                
                // DTO 정보 생성
                DtoInfo dto = DtoInfo.builder()
                    .className(className)
                    .fields(new ArrayList<>())
                    .existingAnnotations(new HashMap<>())
                    .filePath(filePath)
                    .build();
                
                dtoClasses.add(dto);
            }
        }

        /**
         * DTO 클래스 파일 경로 찾기
         */
        private String findDtoFile(String className, String packageName) {
            // 1. 현재 프로젝트의 src/main/java에서 검색
            String foundPath = searchInSourceDirectory(className, packageName);
            if (foundPath != null) {
                return foundPath;
            }
            
            // 2. 패키지명 기반 추정 경로 (fallback)
            String packagePath = packageName.replace('.', '/');
            String estimatedPath = "src/main/java/" + packagePath + "/" + className + ".java";
            
            // 3. 추정 경로가 실제로 존재하는지 확인
            if (Files.exists(Paths.get(estimatedPath))) {
                return estimatedPath;
            }
            
            // 4. 다른 가능한 위치들 검색
            return searchInAlternativeLocations(className, packageName);
        }

        /**
         * 클래스명으로 파일 검색 (패키지명 없이)
         * 더 광범위한 검색을 수행
         */
        private String findDtoFileByClassName(String className) {
            try {
                // 프로젝트 전체에서 해당 클래스명의 파일 검색
                Path projectRootPath = Paths.get(projectRoot);
                return Files.walk(projectRootPath)
                    .filter(path -> path.getFileName().toString().equals(className + ".java"))
                    .findFirst()
                    .map(Path::toString)
                    .orElse(null);
            } catch (IOException e) {
                System.err.println("클래스명으로 파일 검색 중 오류: " + e.getMessage());
                return null;
            }
        }

        /**
         * src/main/java 디렉토리에서 DTO 파일 검색
         */
        private String searchInSourceDirectory(String className, String packageName) {
            try {
                // 프로젝트 루트 기준으로 src/main/java 경로 생성
                Path sourceDir = Paths.get(projectRoot, "src/main/java");
                if (!Files.exists(sourceDir)) {
                    // 상대 경로로도 시도
                    sourceDir = Paths.get("src/main/java");
                    if (!Files.exists(sourceDir)) {
                        return null;
                    }
                }
                
                // 패키지 구조에 따라 검색
                String packagePath = packageName.replace('.', '/');
                Path packageDir = sourceDir.resolve(packagePath);
                
                if (Files.exists(packageDir)) {
                    // 정확한 패키지 디렉토리에서 파일 검색
                    return Files.walk(packageDir)
                        .filter(path -> path.getFileName().toString().equals(className + ".java"))
                        .findFirst()
                        .map(Path::toString)
                        .orElse(null);
                }
                
                // 패키지 구조가 다를 수 있으므로 전체 검색
                return Files.walk(sourceDir)
                    .filter(path -> path.getFileName().toString().equals(className + ".java"))
                    .findFirst()
                    .map(Path::toString)
                    .orElse(null);
                    
            } catch (IOException e) {
                System.err.println("파일 검색 중 오류: " + e.getMessage());
                return null;
            }
        }

        /**
         * 대체 위치들에서 DTO 파일 검색
         */
        private String searchInAlternativeLocations(String className, String packageName) {
            String packagePath = packageName.replace('.', '/');
            String[] alternativePaths = {
                // 프로젝트 루트 기준 경로들
                Paths.get(projectRoot, "src/main/java", packagePath, className + ".java").toString(),
                Paths.get(projectRoot, "src", packagePath, className + ".java").toString(),
                Paths.get(projectRoot, "java", packagePath, className + ".java").toString(),
                Paths.get(projectRoot, "main/java", packagePath, className + ".java").toString(),
                // 상대 경로들
                "src/main/java/" + packagePath + "/" + className + ".java",
                "src/" + packagePath + "/" + className + ".java",
                "java/" + packagePath + "/" + className + ".java",
                "main/java/" + packagePath + "/" + className + ".java"
            };
            
            for (String path : alternativePaths) {
                if (Files.exists(Paths.get(path))) {
                    return path;
                }
            }
            
            // 마지막으로 추정 경로 반환 (존재하지 않더라도)
            return "src/main/java/" + packagePath + "/" + className + ".java";
        }
    }

    /**
     * 타입 파싱 유틸리티 클래스
     * - 제네릭 타입 파싱, 패키지명 제거, 타입 정규화 등 통합 처리
     */
    private static class TypeParser {
        
        /**
         * 타입 파싱 모드
         */
        public enum ParseMode {
            DTO_EXTRACTION,    // DTO 클래스 추출용
            FIELD_NORMALIZATION, // 필드 타입 정규화용
            CLASS_NAME_ONLY     // 클래스명만 추출용
        }
        
        /**
         * 통합 타입 파싱 메서드
         */
        public static TypeParseResult parseType(String typeName, ParseMode mode) {
            if (typeName == null || typeName.isEmpty()) {
                return new TypeParseResult("Object", new ArrayList<>());
            }
            
            // 제네릭 타입 처리
            if (typeName.contains("<") && typeName.contains(">")) {
                return parseGenericType(typeName, mode);
            }
            
            // 단순 타입 처리
            return parseSimpleType(typeName, mode);
        }
        
        /**
         * 제네릭 타입 파싱
         */
        private static TypeParseResult parseGenericType(String typeName, ParseMode mode) {
            String baseType = typeName.substring(0, typeName.indexOf('<'));
            String genericContent = typeName.substring(typeName.indexOf('<') + 1, typeName.lastIndexOf('>'));
            
            // 제네릭 타입들 추출
            List<String> genericTypes = extractGenericTypes(genericContent);
            
            // 모드에 따른 처리
            switch (mode) {
                case DTO_EXTRACTION:
                    return new TypeParseResult(baseType, genericTypes);
                    
                case FIELD_NORMALIZATION:
                    return normalizeGenericType(baseType, genericTypes);
                    
                case CLASS_NAME_ONLY:
                    return new TypeParseResult(extractClassName(baseType), genericTypes);
                    
                default:
                    return new TypeParseResult(baseType, genericTypes);
            }
        }
        
        /**
         * 단순 타입 파싱
         */
        private static TypeParseResult parseSimpleType(String typeName, ParseMode mode) {
            String normalizedType = typeName;
            
            // 패키지명 제거
            if (typeName.contains(".")) {
                normalizedType = typeName.substring(typeName.lastIndexOf('.') + 1);
            }
            
            // 모드별 추가 처리
            if (mode == ParseMode.FIELD_NORMALIZATION) {
                normalizedType = normalizeSimpleType(typeName);
            }
            
            return new TypeParseResult(normalizedType, new ArrayList<>());
        }
        
        /**
         * 제네릭 타입 정규화
         */
        private static TypeParseResult normalizeGenericType(String baseType, List<String> genericTypes) {
            String normalizedBase = normalizeSimpleType(baseType);
            
            if (normalizedBase.equals("List") || normalizedBase.equals("Set")) {
                String genericType = genericTypes.isEmpty() ? "Object" : 
                    parseType(genericTypes.get(0), ParseMode.CLASS_NAME_ONLY).getBaseType();
                return new TypeParseResult(normalizedBase + "<" + genericType + ">", genericTypes);
            }
            
            if (normalizedBase.equals("Map")) {
                return new TypeParseResult("Map<String, Object>", genericTypes);
            }
            
            if (normalizedBase.equals("Optional")) {
                String genericType = genericTypes.isEmpty() ? "Object" : 
                    parseType(genericTypes.get(0), ParseMode.CLASS_NAME_ONLY).getBaseType();
                return new TypeParseResult("Optional<" + genericType + ">", genericTypes);
            }
            
            return new TypeParseResult(normalizedBase, genericTypes);
        }
        
        /**
         * 단순 타입 정규화
         */
        private static String normalizeSimpleType(String typeName) {
            if (typeName.startsWith("java.lang.")) {
                return typeName.substring(10);
            }
            if (typeName.startsWith("java.util.")) {
                return typeName.substring(10);
            }
            if (typeName.contains(".")) {
                return typeName.substring(typeName.lastIndexOf('.') + 1);
            }
            return typeName;
        }
        
        /**
         * 클래스명만 추출
         */
        private static String extractClassName(String typeName) {
            if (typeName.contains(".")) {
                return typeName.substring(typeName.lastIndexOf('.') + 1);
            }
            return typeName;
        }
        
        /**
         * 제네릭 타입들 추출
         */
        private static List<String> extractGenericTypes(String content) {
            List<String> types = new ArrayList<>();
            String[] parts = content.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    types.add(trimmed);
                }
            }
            return types;
        }
    }
    
    /**
     * 타입 파싱 결과 클래스
     */
    private static class TypeParseResult {
        private final String baseType;
        private final List<String> genericTypes;
        
        public TypeParseResult(String baseType, List<String> genericTypes) {
            this.baseType = baseType;
            this.genericTypes = genericTypes;
        }
        
        public String getBaseType() { return baseType; }
        public List<String> getGenericTypes() { return genericTypes; }
    }

    /**
     * AST를 순회하며 DTO 클래스와 필드 정보 추출
     */
    private class DtoVisitor extends VoidVisitorAdapter<Void> {
        private final Path filePath;

        public DtoVisitor(Path filePath) {
            this.filePath = filePath;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            if (isDtoClass(n)) {
                DtoInfo dto = extractDto(n);
                if (dto != null) {
                    dtoClasses.add(dto);
                }
            }
            // 하위 노드들도 방문
            super.visit(n, arg);
        }

        /**
         * DTO 클래스인지 확인 (파일명 패턴으로 이미 필터링됨)
         */
        private boolean isDtoClass(ClassOrInterfaceDeclaration n) {
            // 파일명 패턴으로 이미 필터링되었으므로 true 반환
            return true;
        }

        /**
         * DTO 클래스에서 정보 추출
         */
        private DtoInfo extractDto(ClassOrInterfaceDeclaration n) {
            // 기본 DTO 정보 추출
            String className = n.getNameAsString();
            String filePathStr = filePath.toString();
            
            DtoInfo dto = DtoInfo.builder()
                .className(className)
                .filePath(filePathStr)
                .build();
            
            // 필드 정보 추출
            List<FieldInfo> fields = n.getFields().stream()
                .map(this::extractField)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            dto.setFields(fields);
            return dto;
        }


        /**
         * DTO 필드에서 정보 추출
         * - 필드명, 타입, 검증 어노테이션, 설명, 필수 여부 등을 추출
         */
        private FieldInfo extractField(com.github.javaparser.ast.body.FieldDeclaration field) {
            if (field.getVariables().isEmpty()) {
                return null;
            }
            
            com.github.javaparser.ast.body.VariableDeclarator variable = field.getVariables().get(0);
            String fieldName = variable.getNameAsString();
            String fieldType = TypeParser.parseType(field.getElementType().toString(), TypeParser.ParseMode.FIELD_NORMALIZATION).getBaseType();
            
            // 검증 어노테이션 추출 (확장된 목록)
            List<String> validationAnnotations = field.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .filter(this::isValidationAnnotation)
                .collect(Collectors.toList());
            
            // 필수 여부 확인 (다양한 어노테이션 체크)
            boolean required = isFieldRequired(field);
            
            // 필드 설명 추출 (Javadoc, @Schema, @ApiModelProperty 등)
            String description = extractFieldDescription(field);
            
            return FieldInfo.builder()
                .name(fieldName)
                .type(fieldType)
                .validationAnnotations(validationAnnotations.toArray(new String[0]))
                .description(description)
                .required(required)
                .build();
        }


        /**
         * 검증 어노테이션인지 확인
         */
        private boolean isValidationAnnotation(String annotationName) {
            return annotationName.equals("Valid") || 
                   annotationName.equals("NotNull") || 
                   annotationName.equals("NotBlank") ||
                   annotationName.equals("NotEmpty") ||
                   annotationName.equals("Size") || 
                   annotationName.equals("Min") ||
                   annotationName.equals("Max") ||
                   annotationName.equals("Email") || 
                   annotationName.equals("Pattern") ||
                   annotationName.equals("DecimalMin") ||
                   annotationName.equals("DecimalMax") ||
                   annotationName.equals("Digits") ||
                   annotationName.equals("Future") ||
                   annotationName.equals("Past") ||
                   annotationName.equals("AssertTrue") ||
                   annotationName.equals("AssertFalse");
        }

        /**
         * 필드가 필수인지 확인
         */
        private boolean isFieldRequired(com.github.javaparser.ast.body.FieldDeclaration field) {
            return field.getAnnotations().stream()
                .anyMatch(ann -> {
                    String name = ann.getNameAsString();
                    return name.equals("NotNull") || 
                           name.equals("NotBlank") || 
                           name.equals("NotEmpty") ||
                           name.equals("Required");
                });
        }

        /**
         * 필드 설명 추출
         * - Javadoc, @Schema, @ApiModelProperty 어노테이션에서 설명 추출
         */
        private String extractFieldDescription(com.github.javaparser.ast.body.FieldDeclaration field) {
            // 1. @Schema 어노테이션에서 description 추출
            Optional<String> schemaDescription = field.getAnnotations().stream()
                .filter(ann -> ann.getNameAsString().equals("Schema"))
                .findFirst()
                .map(ann -> extractAnnotationValue(ann, "description"));
            
            if (schemaDescription.isPresent() && !schemaDescription.get().isEmpty()) {
                return schemaDescription.get();
            }
            
            // 2. @ApiModelProperty 어노테이션에서 value 추출
            Optional<String> apiModelDescription = field.getAnnotations().stream()
                .filter(ann -> ann.getNameAsString().equals("ApiModelProperty"))
                .findFirst()
                .map(ann -> extractAnnotationValue(ann, "value"));
            
            if (apiModelDescription.isPresent() && !apiModelDescription.get().isEmpty()) {
                return apiModelDescription.get();
            }
            
            // 3. Javadoc에서 설명 추출
            return field.getJavadoc()
                .map(javadoc -> javadoc.getDescription().toText().trim())
                .filter(desc -> !desc.isEmpty())
                .orElse("");
        }

        /**
         * 어노테이션에서 특정 속성값 추출
         */
        private String extractAnnotationValue(AnnotationExpr annotation, String attributeName) {
            if (annotation.isNormalAnnotationExpr()) {
                com.github.javaparser.ast.expr.NormalAnnotationExpr normalAnn = 
                    annotation.asNormalAnnotationExpr();
                return normalAnn.getPairs().stream()
                    .filter(pair -> pair.getNameAsString().equals(attributeName))
                    .findFirst()
                    .map(pair -> pair.getValue().toString().replaceAll("\"", ""))
                    .orElse("");
            }
            return "";
        }

    }
}



