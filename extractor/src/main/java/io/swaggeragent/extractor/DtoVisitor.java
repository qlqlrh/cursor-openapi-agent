package io.swaggeragent.extractor;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import io.swaggeragent.extractor.model.*;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

/**
 * AST를 순회하며 DTO 클래스와 필드 정보를 추출하는 Visitor
 */
@AllArgsConstructor
public class DtoVisitor extends VoidVisitorAdapter<Void> {
    private final Path filePath;
    private final Set<DtoInfo> dtoClasses;

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        DtoInfo dto = extractDto(n);
        if (dto != null) {
            dtoClasses.add(dto);
        }
        // 하위 노드들도 방문
        super.visit(n, arg);
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
