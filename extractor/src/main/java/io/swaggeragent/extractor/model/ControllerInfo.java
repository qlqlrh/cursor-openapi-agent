package io.swaggeragent.extractor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * 컨트롤러 클래스 정보를 저장하는 모델 클래스
 * - 컨트롤러의 기본 정보와 메서드 목록 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControllerInfo {
    private String className;
    
    private String requestMapping;
    
    private List<MethodInfo> methods;
    
    private Map<String, Object> existingAnnotations;
}