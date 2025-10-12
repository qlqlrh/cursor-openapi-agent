#!/usr/bin/env bash
set -e

# 현재 프로젝트 루트 (Spring Boot 프로젝트)
PROJECT_ROOT="$(pwd)"
# Cursor OpenAPI Agent 루트 (도구가 설치된 위치)
AGENT_ROOT="$PROJECT_ROOT/cursor-openapi-agent"

EXTRACTOR_JAR="$AGENT_ROOT/extractor/build/libs/swagger-agent-extractor.jar"
OUTPUT_FILE="$AGENT_ROOT/out/endpoints.json"
SOURCE_PATH="$PROJECT_ROOT/src/main/java"

echo "🔍 Extracting controller metadata..."
echo "Source: $SOURCE_PATH"
echo "Output: $OUTPUT_FILE"

# Check if src/main/java directory exists
if [ ! -d "$SOURCE_PATH" ]; then
    echo "❌ 추출할 메타데이터가 없습니다"
    echo "   $SOURCE_PATH 폴더가 존재하지 않습니다."
    echo "   이 도구는 Spring Boot 프로젝트에서 사용하도록 설계되었습니다."
    echo "   Spring Boot 프로젝트의 루트 디렉토리에서 실행해주세요."
    exit 0
fi

# Create output directory if it doesn't exist
mkdir -p "$(dirname "$OUTPUT_FILE")"

# Build extractor if jar doesn't exist
if [ ! -f "$EXTRACTOR_JAR" ]; then
    echo "📦 Building extractor..."
    cd "$AGENT_ROOT/extractor"
    gradle build -q
    cd "$PROJECT_ROOT"
fi

# Run extraction
java -cp "$EXTRACTOR_JAR" io.swaggeragent.extractor.Main \
  --src="$SOURCE_PATH" \
  --out="$OUTPUT_FILE"

echo "Extraction completed successfully!"
echo "Found $(jq '.totalMethods' "$OUTPUT_FILE") methods in $(jq '.controllers | length' "$OUTPUT_FILE") controllers"
echo "Output written to: $OUTPUT_FILE"
echo "✅ Extraction completed!"
