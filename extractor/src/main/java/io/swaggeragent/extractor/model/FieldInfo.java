package io.swaggeragent.extractor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO 필드 정보를 저장하는 모델 클래스
 * - 필드명, 타입, 검증 어노테이션 등 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldInfo {
    private String name;
    
    private String type;
    
    private String[] validationAnnotations;
    
    private String description;
    
    private boolean required;
}
