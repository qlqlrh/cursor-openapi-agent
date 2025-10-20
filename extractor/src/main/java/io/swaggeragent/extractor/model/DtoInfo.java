package io.swaggeragent.extractor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * DTO 클래스 정보를 저장하는 모델 클래스
 * - DTO 클래스의 기본 정보와 필드 정보 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoInfo {
    private String className;
    
    private List<FieldInfo> fields;
    
    private Map<String, Object> existingAnnotations;
    
    private String filePath;
}
