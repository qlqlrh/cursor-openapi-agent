# ✨ Cursor OpenAPI Agent

Spring Boot 프로젝트에서 **OpenAPI/Swagger 주석을 자동으로 생성**하는 Cursor IDE 전용 도구입니다.  
JavaParser를 사용하여 코드를 분석하고, LLM을 통해 최적화된 한국어 주석을 생성/적용합니다.  


## 🚀 설치 (프로젝트에 Agent 추가)

### 🛠️ 자동 설치 (권장)
Spring Boot 프로젝트 루트 디렉토리에서 다음 명령어를 실행하면, `cursor-openapi-agent` 폴더와 `.cursor/commands` 파일이 자동으로 설정됩니다.
```bash
# Spring Boot 프로젝트 루트에서 실행
curl -sSL https://raw.githubusercontent.com/qlqlrh/cursor-openapi-agent/main/install.sh | bash
```

### ✍️ 수동 설치
프로젝트 루트에 직접 파일을 복사하여 설정합니다.
```bash
# 1. 저장소 클론
git clone https://github.com/qlqlrh/cursor-openapi-agent.git cursor-openapi-agent

# 2. Cursor 명령어 설정
mkdir -p .cursor/commands
cp cursor-openapi-agent/.cursor/commands/* .cursor/commands/

# 3. 실행 권한 부여
chmod +x cursor-openapi-agent/scripts/run_extract.sh
```

## 💡 사용 방법
설치 후, Cursor IDE 내에서 다음 명령어를 사용하여 Swagger 주석을 관리합니다.

### 1. 메타데이터 추출 
컨트롤러 및 DTO 메타데이터를 추출하여 LLM 분석에 필요한 cursor-openapi-agent/out/endpoints.json 파일을 생성합니다.  



**전체 스캔 (모든 컨트롤러 및 DTO)**  
- `src/main/java`의 모든 컨트롤러/DTO를 스캔합니다.
```
/swg-extract
```


**선택적 스캔 (특정 파일)**  
- 파일 이름을 @ 기호와 함께 사용하여 특정 컨트롤러 또는 DTO만 스캔합니다.
```
# 컨트롤러와 연관된 DTO까지 스캔
/swg-extract @UserController.java

# 특정 DTO 파일만 스캔
/swg-extract @UserDto.java

# 여러 파일 동시 스캔
/swg-extract @UserController.java @TripController.java @CommonDto.java
```

### 2. Swagger 주석 적용
추출된 메타데이터를 바탕으로 OpenAPI(Swagger) 주석을 생성하고 파일에 적용합니다.
```
/swg-apply
```
- **컨트롤러**: `@Operation`, `@ApiResponse`, `@Parameter` 등 자동 생성
- **DTO**: `@Schema` 자동 생성
- Cursor IDE에서 Accept/Reject 선택이 가능합니다

### 🎯 사용 시나리오
| 상황 | 명령 순서 | 설명 |
|---|---|---|
| 새로운 컨트롤러/API 추가 | `/swg-extract @NewController.java` → `/swg-apply` | 컨트롤러 및 연관 DTO의 주석을 한 번에 생성합니다. |
| DTO 필드 수정 | `/swg-extract @ModifiedDto.java` → `/swg-apply` | 수정된 DTO의 `@Schema` 주석만 업데이트합니다. |
| 전체 프로젝트 업데이트 | `/swg-extract` → `/swg-apply` | 프로젝트 전체의 모든 누락/변경 사항을 스캔하고 적용합니다. |


## 📁 프로젝트 구조

### 설치 후 사용자 프로젝트 구조
```
your-spring-project/
├── src/main/java/          # 사용자의 Spring Boot 컨트롤러
│   └── com/example/controller/
├── cursor-openapi-agent/   # 설치된 Agent
└── .cursor/commands/       # Cursor 명령어 (프로젝트 루트에 설치)
    ├── swg-extract.md      # 메타데이터 추출 명령
    └── swg-apply.md        # Swagger 주석 적용 명령
```

### Agent 구조
```
cursor-openapi-agent/
├── extractor/              # 컨트롤러 메타데이터 추출기 (JavaParser)
│   ├── src/main/java/io/swaggeragent/extractor/
│   │   ├── Main.java
│   │   ├── ControllerExtractor.java    # 메인 추출기 (리팩토링됨)
│   │   ├── ControllerVisitor.java      # 컨트롤러 AST 방문자
│   │   ├── DtoVisitor.java            # DTO AST 방문자
│   │   ├── TypeParser.java            # 타입 파싱 유틸리티
│   │   └── model/
│   │       ├── ControllerInfo.java     # 컨트롤러 기본 정보
│   │       ├── MethodInfo.java         # HTTP 메소드/경로/응답/예외
│   │       ├── ParameterInfo.java      # 파라미터 이름/타입/위치/필수 여부
│   │       ├── DtoInfo.java            # DTO 클래스 정보 및 경로
│   │       ├── FieldInfo.java          # DTO 필드 타입/검증/필수 여부
│   │       ├── EndpointsInfo.java      # 추출 결과 루트(컨트롤러/DTO/통계)
│   │       ├── FileProcessResult.java  # 파일 처리 결과
│   │       └── TypeParseResult.java    # 타입 파싱 결과
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

## 🔧 트러블슈팅

### Extractor 빌드 오류
```bash
cd cursor-openapi-agent/extractor
./gradlew clean build
```

### `/swg-extract` 명령이 작동하지 않음
- Cursor에서 명령어가 인식되지 않는 경우: `.cursor/commands/` 폴더가 프로젝트 루트에 있는지 확인
- Java 17+ 설치 확인: `java -version`
- Gradle 빌드 확인: `cd cursor-openapi-agent/extractor && ./gradlew build`

### `/swg-apply` 명령이 작동하지 않음
- 먼저 `/swg-extract`를 실행했는지 확인
- `cursor-openapi-agent/out/endpoints.json` 파일이 존재하는지 확인
- Cursor IDE에서 Accept/Reject 선택이 나타나는지 확인

### 주석이 적용되지 않음
- Cursor IDE에서 변경사항을 Accept했는지 확인
- 파일이 수정되었는지 확인
- 기존 주석과 충돌이 없는지 확인

## 📝 주석 예시

### Controller
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

### DTO

```java
public class TripCreateRequest {

  @Schema(description = "여행 제목", example = "제주도 3박 4일 여행", required = true, maxLength = 100)
  @NotBlank(message = "여행 제목은 필수입니다")
  @Size(max = 100, message = "여행 제목은 100자 이하여야 합니다")
  private String title;

  @Schema(description = "여행 설명", example = "가족과 함께하는 즐거운 제주도 여행", maxLength = 500)
  @Size(max = 500, message = "여행 설명은 500자 이하여야 합니다")
  private String description;

  @Schema(description = "여행 시작일", example = "2024-03-15", required = true)
  @NotNull(message = "시작일은 필수입니다")
  private LocalDate startDate;

  @Schema(description = "여행 종료일", example = "2024-03-18", required = true)
  @NotNull(message = "종료일은 필수입니다")
  private LocalDate endDate;

  @Schema(description = "여행 목적지", example = "제주도")
  private String destination;

  @Schema(description = "여행 예산 (원)", example = "500000")
  private Integer budget;
}
```


## 📄 라이선스

MIT License
