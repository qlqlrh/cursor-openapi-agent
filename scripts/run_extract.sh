#!/usr/bin/env bash
set -e

# 현재 프로젝트 루트 (Spring Boot 프로젝트)
PROJECT_ROOT="$(pwd)"
# Cursor OpenAPI Agent 루트 (도구가 설치된 위치)
AGENT_ROOT="$PROJECT_ROOT/cursor-openapi-agent"

EXTRACTOR_JAR="$AGENT_ROOT/extractor/build/libs/swagger-agent-extractor.jar"
OUTPUT_FILE="$AGENT_ROOT/out/endpoints.json"
SOURCE_PATH="$PROJECT_ROOT/src/main/java"

# 파라미터 파싱
FILES_MODE=false
SELECTED_FILES=()

# 명령행 인수 파싱
while [[ $# -gt 0 ]]; do
    case $1 in
        --files)
            FILES_MODE=true
            shift
            # --files 다음의 모든 인수를 파일 목록으로 처리
            while [[ $# -gt 0 && ! "$1" =~ ^-- ]]; do
                SELECTED_FILES+=("$1")
                shift
            done
            ;;
        -h|--help)
            echo "사용법: $0 [--files 파일1 파일2 ...]"
            echo ""
            echo "옵션:"
            echo "  --files 파일1 파일2 ...    특정 파일들에서만 메타데이터 추출"
            echo "  -h, --help                 이 도움말 표시"
            echo ""
            echo "예시:"
            echo "  $0                         # src/main/java 전체 스캔"
            echo "  $0 --files UserController.java UserDto.java  # 선택된 파일만"
            exit 0
            ;;
        *)
            echo "알 수 없는 옵션: $1"
            echo "사용법을 보려면 --help를 사용하세요"
            exit 1
            ;;
    esac
done

# 모드에 따른 메시지 출력
if [ "$FILES_MODE" = true ]; then
    echo "🔍 선택된 파일들에서 메타데이터 추출 중..."
    echo "선택된 파일: ${SELECTED_FILES[*]}"
else
    echo "🔍 컨트롤러 메타데이터 추출 중 (전체 스캔)..."
    echo "소스: $SOURCE_PATH"
fi
echo "출력: $OUTPUT_FILE"

# 파일 모드인 경우 선택된 파일들이 존재하는지 확인
if [ "$FILES_MODE" = true ]; then
    if [ ${#SELECTED_FILES[@]} -eq 0 ]; then
        echo "❌ 선택된 파일이 없습니다"
        echo "   사용법: $0 --files 파일1 파일2 ..."
        exit 1
    fi
    
    # 선택된 파일들이 존재하는지 확인
    for file in "${SELECTED_FILES[@]}"; do
        if [ ! -f "$file" ]; then
            echo "❌ 파일을 찾을 수 없습니다: $file"
            exit 1
        fi
    done
else
    # 전체 스캔 모드인 경우 src/main/java 디렉토리 확인
    if [ ! -d "$SOURCE_PATH" ]; then
        echo "❌ 추출할 메타데이터가 없습니다"
        echo "   $SOURCE_PATH 폴더가 존재하지 않습니다."
        echo "   이 도구는 Spring Boot 프로젝트에서 사용하도록 설계되었습니다."
        echo "   Spring Boot 프로젝트의 루트 디렉토리에서 실행해주세요."
        exit 0
    fi
fi

# Create output directory if it doesn't exist
mkdir -p "$(dirname "$OUTPUT_FILE")"

# Build extractor if jar doesn't exist
if [ ! -f "$EXTRACTOR_JAR" ]; then
    echo "📦 추출기 빌드 중..."
    cd "$AGENT_ROOT/extractor"
    gradle build -q
    cd "$PROJECT_ROOT"
fi

# Run extraction
if [ "$FILES_MODE" = true ]; then
    # 선택적 파일 모드
    java -cp "$EXTRACTOR_JAR" io.swaggeragent.extractor.Main \
      --files "${SELECTED_FILES[@]}" \
      --out="$OUTPUT_FILE"
else
    # 전체 스캔 모드
    java -cp "$EXTRACTOR_JAR" io.swaggeragent.extractor.Main \
      --src="$SOURCE_PATH" \
      --out="$OUTPUT_FILE"
fi

echo "메타데이터 추출이 성공적으로 완료되었습니다!"
if [ -f "$OUTPUT_FILE" ]; then
    echo "$(jq '.controllers | length' "$OUTPUT_FILE")개 컨트롤러에서 $(jq '.totalMethods' "$OUTPUT_FILE")개 메소드를 찾았습니다"
    if [ "$FILES_MODE" = true ] && [ "$(jq '.dtoClasses | length' "$OUTPUT_FILE" 2>/dev/null || echo 0)" -gt 0 ]; then
        echo "$(jq '.dtoClasses | length' "$OUTPUT_FILE")개 DTO 클래스를 찾았습니다"
    fi
fi
echo "결과가 저장되었습니다: $OUTPUT_FILE"
echo "✅ 추출 완료!"
