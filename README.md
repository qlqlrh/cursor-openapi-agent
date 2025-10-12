# Cursor OpenAPI Agent – 자동 OpenAPI/Swagger 주석 생성기

Spring Boot 컨트롤러에서 자동으로 OpenAPI/Swagger 주석을 생성하는 Cursor 전용 도구입니다.

## 🚀 빠른 시작

### 다른 프로젝트에서 사용하기

#### 자동 설치 (권장)
```bash
# Spring Boot 프로젝트 루트에서 실행
curl -sSL https://raw.githubusercontent.com/qlqlrh/cursor-openapi-agent/main/install.sh | bash
```

#### 수동 설치
```bash
# 1. 저장소 클론
git clone https://github.com/qlqlrh/cursor-openapi-agent.git cursor-openapi-agent

# 2. Cursor 명령어 설정
mkdir -p .cursor/commands
cp cursor-openapi-agent/.cursor/commands/* .cursor/commands/

# 3. 실행 권한 부여
chmod +x cursor-openapi-agent/scripts/run_extract.sh
```

### Cursor 명령어 사용

#### 1. 메타데이터 추출
```
/swg-extract
```
- `src/main/java`의 모든 컨트롤러를 스캔하여 `cursor-openapi-agent/out/endpoints.json`을 생성합니다
- LLM이 결과를 분석하여 보기 좋게 출력합니다

#### 2. Swagger 주석 적용
```
/swg-apply
```
- 추출된 메타데이터를 기반으로 Swagger 주석을 생성하고 적용합니다
- Cursor IDE에서 Accept/Reject 선택이 가능합니다


## 📁 프로젝트 구조

### 설치 후 사용자 프로젝트 구조
```
your-spring-project/
├── src/main/java/          # 사용자의 Spring Boot 컨트롤러
│   └── com/example/controller/
├── cursor-openapi-agent/   # 설치된 도구
│   ├── extractor/          # 컨트롤러 메타데이터 추출기 (JavaParser)
│   │   ├── src/main/java/io/swaggeragent/extractor/
│   │   │   ├── Main.java
│   │   │   ├── ControllerExtractor.java
│   │   │   └── model/
│   │   └── build.gradle
│   ├── out/                # 출력 파일
│   │   └── endpoints.json  # 추출된 메타데이터
│   ├── scripts/            # 실행 스크립트
│   │   └── run_extract.sh
│   ├── install.sh          # 자동 설치 스크립트
│   ├── LICENSE             # MIT 라이선스
│   └── README.md
└── .cursor/commands/       # Cursor 명령어 (프로젝트 루트에 설치)
    ├── swg-extract.md      # 메타데이터 추출 명령
    └── swg-apply.md        # Swagger 주석 적용 명령
```

### 도구 자체 구조
```
cursor-openapi-agent/
├── extractor/              # 컨트롤러 메타데이터 추출기 (JavaParser)
│   ├── src/main/java/io/swaggeragent/extractor/
│   │   ├── Main.java
│   │   ├── ControllerExtractor.java
│   │   └── model/
│   └── build.gradle
├── out/                    # 출력 파일
│   └── endpoints.json      # 추출된 메타데이터
├── scripts/                # 실행 스크립트
│   └── run_extract.sh
├── .cursor/commands/       # Cursor 명령어
│   ├── swg-extract.md      # 메타데이터 추출 명령
│   └── swg-apply.md        # Swagger 주석 적용 명령
├── install.sh              # 자동 설치 스크립트
├── LICENSE                 # MIT 라이선스
└── README.md
```

## ⚙️ 설정

현재는 Cursor LLM이 자동으로 최적화된 설정을 사용합니다

- **언어**: 한국어 주석 자동 생성
- **태그**: 컨트롤러명 기반 자동 생성
- **응답 코드**: 표준 HTTP 상태 코드 자동 적용
- **병합 규칙**: 기존 주석과 안전하게 병합
- **스캔 범위**: `src/main/java`의 모든 컨트롤러 파일


## 🔧 트러블슈팅

### Extractor 빌드 오류
```bash
cd cursor-openapi-agent/extractor
gradle clean build
```

### `/swg-extract` 명령이 작동하지 않음
- Cursor에서 명령어가 인식되지 않는 경우: `.cursor/commands/` 폴더가 프로젝트 루트에 있는지 확인
- Java 17+ 설치 확인: `java -version`
- Gradle 빌드 확인: `cd cursor-openapi-agent/extractor && gradle build`

### `/swg-apply` 명령이 작동하지 않음
- 먼저 `/swg-extract`를 실행했는지 확인
- `cursor-openapi-agent/out/endpoints.json` 파일이 존재하는지 확인
- Cursor IDE에서 Accept/Reject 선택이 나타나는지 확인

### 주석이 적용되지 않음
- Cursor IDE에서 변경사항을 Accept했는지 확인
- 파일이 수정되었는지 확인
- 기존 주석과 충돌이 없는지 확인

## 📝 주석 예시

생성되는 주석 예시

```java
@Operation(
  summary = "여행 상세 조회",
  description = "여행 ID로 특정 여행의 상세 정보를 조회합니다. 존재하지 않는 여행 ID일 경우 404를 반환합니다.",
  tags = {"여행"}
)
@ApiResponses(value = {
  @ApiResponse(responseCode = "200", description = "여행 조회 성공"),
  @ApiResponse(responseCode = "404", description = "여행을 찾을 수 없음"),
  @ApiResponse(responseCode = "500", description = "서버 오류")
})
@GetMapping("/{trip_id}")
public ResponseEntity<FindTripResponse> getTripById(
  @Parameter(name="tripId", in=ParameterIn.PATH, required=true, description="여행 식별자")
  @PathVariable Long tripId
) { ... }
```


## 📄 라이선스

MIT License

