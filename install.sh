#!/bin/bash
set -e

echo "🚀 Cursor OpenAPI Agent 설치 중..."

# 현재 프로젝트 루트 확인
if [ ! -f "build.gradle" ] && [ ! -f "pom.xml" ] && [ ! -f "package.json" ]; then
    echo "❌ 오류: Spring Boot 프로젝트 루트에서 실행해주세요."
    echo "   (build.gradle, pom.xml, package.json 중 하나가 있는 폴더에서 실행)"
    exit 1
fi

# cursor-openapi-agent 폴더 생성
mkdir -p cursor-openapi-agent

# 저장소 클론 또는 업데이트
if [ ! -d "cursor-openapi-agent/.git" ]; then
    echo "📥 Cursor OpenAPI Agent 저장소 클론 중..."
    git clone https://github.com/qlqlrh/cursor-openapi-agent.git cursor-openapi-agent
else
    echo "🔄 Cursor OpenAPI Agent 업데이트 중..."
    cd cursor-openapi-agent
    git pull origin main
    cd ..
fi

# .cursor/commands 폴더 생성
mkdir -p .cursor/commands

# 명령어 파일 복사
echo "📋 Cursor 명령어 파일 복사 중..."
cp cursor-openapi-agent/.cursor/commands/* .cursor/commands/

# 실행 권한 부여
chmod +x cursor-openapi-agent/scripts/run_extract.sh

# out 폴더 생성 (endpoints.json 저장용)
mkdir -p cursor-openapi-agent/out

echo ""
echo "✅ 설치 완료!"
echo ""
echo "📖 사용법:"
echo "   /swg-extract  - 메타데이터 추출"
echo "   /swg-apply    - Swagger 주석 적용"
echo ""
echo "📚 자세한 사용법은 cursor-openapi-agent/README.md를 참고하세요."
echo ""
echo "🔧 설정:"
echo "   - 추출된 메타데이터는 cursor-openapi-agent/out/endpoints.json에 저장됩니다"
echo "   - src/main/java 폴더의 컨트롤러를 자동으로 스캔합니다"
echo "   - 한국어 주석과 표준 HTTP 응답 코드가 자동 생성됩니다"
