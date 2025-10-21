package io.swaggeragent.extractor.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 타입 파싱 결과 클래스
 */
@Getter
@AllArgsConstructor
public class TypeParseResult {

    private final String baseType;

    private final List<String> genericTypes;

}
