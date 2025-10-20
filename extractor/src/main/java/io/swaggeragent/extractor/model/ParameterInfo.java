package io.swaggeragent.extractor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 메서드 파라미터 정보를 저장하는 모델 클래스
 * - 파라미터명, 타입, 위치, 검증 어노테이션 등 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterInfo {
    private String name;
    
    private String type;
    
    private String in; // path, query, body, header
    
    private boolean required;
    
    private String[] validationAnnotations;
    
    private String description;
}



