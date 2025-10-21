package io.swaggeragent.extractor;

import io.swaggeragent.extractor.model.TypeParseResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 타입 파싱 유틸리티 클래스
 * - 제네릭 타입 파싱, 패키지명 제거, 타입 정규화 등 통합 처리
 */
public class TypeParser {
    
    /**
     * 타입 파싱 모드
     */
    public enum ParseMode {
        DTO_EXTRACTION,      // DTO 클래스 추출용
        FIELD_NORMALIZATION, // 필드 타입 정규화용
        CLASS_NAME_ONLY      // 클래스명만 추출용
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
