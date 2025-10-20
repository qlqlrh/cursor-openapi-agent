package io.swaggeragent.extractor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * 컨트롤러 메서드 정보를 저장하는 모델 클래스
 * - HTTP 메서드, 경로, 파라미터, 반환 타입 등 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MethodInfo {
    private String methodName;
    
    private String httpMethod;
    
    private String path;
    
    private List<ParameterInfo> parameters;
    
    private String returnType;
    
    private List<String> exceptions;
    
    private Map<String, Object> existingAnnotations;
    
    private int lineNumber;
}



