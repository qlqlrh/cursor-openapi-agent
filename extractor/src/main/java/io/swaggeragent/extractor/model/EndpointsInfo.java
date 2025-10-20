package io.swaggeragent.extractor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 추출된 엔드포인트 데이터를 저장하는 모델 클래스
 * - 컨트롤러 정보와 DTO 클래스 정보 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointsInfo {
    private List<ControllerInfo> controllers;
    
    private List<DtoInfo> dtoClasses;
    
    private String extractedAt;
    
    private int totalMethods;
    
    private int totalDtoClasses;

    public static EndpointsInfo ofControllers(List<ControllerInfo> controllers) {
        return EndpointsInfo.builder()
            .controllers(controllers)
            .dtoClasses(List.of())
            .extractedAt(java.time.LocalDateTime.now().toString())
            .totalMethods(controllers.stream()
                .mapToInt(c -> c.getMethods() != null ? c.getMethods().size() : 0)
                .sum())
            .totalDtoClasses(0)
            .build();
    }

    public static EndpointsInfo ofControllersAndDtos(List<ControllerInfo> controllers, List<DtoInfo> dtoClasses) {
        List<DtoInfo> safeDtoClasses = dtoClasses != null ? dtoClasses : List.of();
        return EndpointsInfo.builder()
            .controllers(controllers)
            .dtoClasses(safeDtoClasses)
            .extractedAt(java.time.LocalDateTime.now().toString())
            .totalMethods(controllers.stream()
                .mapToInt(c -> c.getMethods() != null ? c.getMethods().size() : 0)
                .sum())
            .totalDtoClasses(safeDtoClasses.size())
            .build();
    }

}