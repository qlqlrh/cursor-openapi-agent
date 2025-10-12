package io.swaggeragent.extractor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import io.swaggeragent.extractor.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ControllerExtractor {
    private final JavaParser javaParser;
    private final List<ControllerInfo> controllers;

    public ControllerExtractor() {
        this.javaParser = new JavaParser();
        this.controllers = new ArrayList<>();
    }

    public EndpointsData extract(String sourcePath) throws IOException {
        Path sourceDir = Paths.get(sourcePath);
        Files.walk(sourceDir)
            .filter(path -> path.toString().endsWith(".java"))
            .filter(path -> path.toString().contains("controller"))
            .forEach(this::processFile);
        
        return new EndpointsData(controllers);
    }

    private void processFile(Path filePath) {
        try {
            CompilationUnit cu = javaParser.parse(filePath).getResult().orElse(null);
            if (cu == null) return;

            cu.accept(new ControllerVisitor(filePath), null);
        } catch (Exception e) {
            System.err.println("Error processing file: " + filePath + " - " + e.getMessage());
        }
    }

    private class ControllerVisitor extends VoidVisitorAdapter<Void> {
        private final Path filePath;

        public ControllerVisitor(Path filePath) {
            this.filePath = filePath;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            if (isController(n)) {
                ControllerInfo controller = extractController(n);
                if (controller != null) {
                    controllers.add(controller);
                }
            }
            super.visit(n, arg);
        }

        private boolean isController(ClassOrInterfaceDeclaration n) {
            return n.getAnnotations().stream()
                .anyMatch(ann -> ann.getNameAsString().equals("RestController") || 
                               ann.getNameAsString().equals("Controller"));
        }

        private ControllerInfo extractController(ClassOrInterfaceDeclaration n) {
            String className = n.getNameAsString();
            String packageName = extractPackageName(n);
            String requestMapping = extractRequestMapping(n);
            
            ControllerInfo controller = new ControllerInfo(className, packageName, requestMapping);
            
            List<MethodInfo> methods = n.getMethods().stream()
                .filter(this::isMappingMethod)
                .map(this::extractMethod)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            controller.setMethods(methods);
            return controller;
        }

        private String extractPackageName(ClassOrInterfaceDeclaration n) {
            return n.findCompilationUnit()
                .map(cu -> cu.getPackageDeclaration()
                    .map(pkg -> pkg.getNameAsString())
                    .orElse(""))
                .orElse("");
        }

        private String extractRequestMapping(ClassOrInterfaceDeclaration n) {
            return n.getAnnotations().stream()
                .filter(ann -> ann.getNameAsString().equals("RequestMapping"))
                .findFirst()
                .map(this::extractAnnotationValue)
                .orElse("");
        }

        private String extractAnnotationValue(AnnotationExpr annotation) {
            if (annotation.isSingleMemberAnnotationExpr()) {
                return annotation.asSingleMemberAnnotationExpr()
                    .getMemberValue()
                    .asStringLiteralExpr()
                    .getValue();
            } else if (annotation.isNormalAnnotationExpr()) {
                return annotation.asNormalAnnotationExpr()
                    .getPairs().stream()
                    .filter(pair -> pair.getNameAsString().equals("value"))
                    .findFirst()
                    .map(pair -> pair.getValue().asStringLiteralExpr().getValue())
                    .orElse("");
            }
            return "";
        }

        private boolean isMappingMethod(MethodDeclaration n) {
            return n.getAnnotations().stream()
                .anyMatch(ann -> {
                    String name = ann.getNameAsString();
                    return name.equals("GetMapping") || name.equals("PostMapping") || 
                           name.equals("PutMapping") || name.equals("DeleteMapping") ||
                           name.equals("RequestMapping");
                });
        }

        private MethodInfo extractMethod(MethodDeclaration n) {
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

        private String extractHttpMethod(MethodDeclaration n) {
            return n.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .filter(name -> name.endsWith("Mapping"))
                .map(name -> name.replace("Mapping", "").toUpperCase())
                .findFirst()
                .orElse("GET");
        }

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

        private List<ParameterInfo> extractParameters(MethodDeclaration n) {
            return n.getParameters().stream()
                .map(param -> {
                    String name = param.getNameAsString();
                    String type = param.getType().toString();
                    String in = determineParameterIn(param);
                    boolean required = isParameterRequired(param);
                    
                    ParameterInfo paramInfo = new ParameterInfo(name, type, in, required);
                    paramInfo.setValidationAnnotations(extractValidationAnnotations(param));
                    return paramInfo;
                })
                .collect(Collectors.toList());
        }

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

        private String[] extractValidationAnnotations(com.github.javaparser.ast.body.Parameter param) {
            return param.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .filter(name -> name.equals("Valid") || name.equals("NotNull") || 
                              name.equals("Size") || name.equals("NotBlank") ||
                              name.equals("RequestBody") || name.equals("PathVariable") ||
                              name.equals("RequestParam") || name.equals("RequestHeader"))
                .toArray(String[]::new);
        }

        private List<String> extractExceptions(MethodDeclaration n) {
            return n.getThrownExceptions().stream()
                .map(exception -> exception.toString())
                .collect(Collectors.toList());
        }
    }
}



