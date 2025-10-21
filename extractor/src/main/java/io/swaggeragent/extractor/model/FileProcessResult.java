package io.swaggeragent.extractor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 파일 처리 결과를 담는 클래스
 */
@Getter
@AllArgsConstructor
public class FileProcessResult {
    
    private final boolean success;

    private final boolean controllerFile;
    
    private final boolean dtoFile;
    
    private final String errorMessage;
    
    public static FileProcessResult success(boolean controllerFile, boolean dtoFile) {
        return new FileProcessResult(true, controllerFile, dtoFile, null);
    }
    
    public static FileProcessResult error(String errorMessage) {
        return new FileProcessResult(false, false, false, errorMessage);
    }
}