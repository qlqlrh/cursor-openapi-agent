# Cursor 전용 자동 스웨거 주석 생성 에이전트 (Context + TODO)
> 이 문서는 **Cursor에게 학습시킬 프롬프트 자료**이자, 개발자가 그대로 따라 하면 **데모까지 완주**할 수 있는 실행 문서입니다.  
> 대상 레포(파일럿): `https://github.com/tkddn204/fastcampus-spring-toy-project`  
> 목표 명령: `/swg-dryrun`, `/swg-apply`

---

## 1) 상황 설명(문맥)
- **배경**: 팀 내 스프링 부트 프로젝트에서 Swagger/OpenAPI 주석(`@Operation`, `@ApiResponses`, `@Parameter`)이 부족하여 문서 품질·일관성이 떨어짐.
- **목표**: 컨트롤러 메타데이터(AST/리플렉션)를 기반으로 **자동으로 주석을 생성/갱신**하는 **Cursor 전용 로컬 에이전트** 제작.
- **데드라인**: 내일 팀 미팅에서 실행 데모 필요.
- **제약/원칙**
  - 로컬 실행(네트워크/외부키 의존 최소화).
  - **멱등성 보장**(중복 삽입/충돌 없이 갱신).
  - 누구나 자신의 스프링 부트 리포에 **쉽게 적용** 가능(오픈소스화 고려).
- **Definition of Done**
  - 명령 제공: `/swg-apply`(실제 삽입).
  - 최소 `@Operation`, `@ApiResponses`, `@Parameter`가 합리적으로 붙는다.
  - 재실행 시에도 중복 없이 업데이트된다.
  - 파일럿 레포에서 빌드/Swagger UI 확인 가능.

---

## 2) 시스템 개요
### 2.1 구성 요소 (현재 구현)
- **Extractor**: `src/main/java`를 스캔하여 컨트롤러/메소드/파라미터/반환/예외/기존 주석 여부를 JSON(`endpoints.json`)으로 추출.
- **Cursor LLM**: `endpoints.json`을 읽고 직접 소스 코드에 Swagger 주석을 추가 (한국어 summary/description, 표준 응답, 파라미터).
- **Templates/Config**: 템플릿(`templates/*.txt`)과 정책(`swagger-agent.yml`)으로 프로젝트별 커스터마이즈.

### 2.2 데이터 흐름 (현재 구현)
```
[Java Source] --(Extractor)--> endpoints.json --(Cursor LLM)--> [Java Source Patched]
```

---

## 3) 폴더 구조(현재)
```
tools/swagger-agent/
├─ extractor/                # 엔드포인트 추출기(Java, JavaParser)
├─ out/                      # endpoints.json
├─ scripts/                  # run_extract.sh
└─ README.md

.cursor/commands/
├─ swg-extract               # 메타데이터 추출 명령 (실행 파일)
├─ swg-extract.md            # 메타데이터 추출 명령 (문서)
└─ swg-apply.md              # 실제 적용 + IDE Accept/Reject 명령
```

---

## 4) 현재 설정 (Cursor LLM 자동 최적화)

현재는 별도의 설정 파일 없이 Cursor LLM이 자동으로 최적화된 설정을 사용합니다:

- **언어**: 한국어 주석 자동 생성
- **태그**: 컨트롤러명 기반 자동 생성 (MemberController → "Member")
- **응답 코드**: 표준 HTTP 상태 코드 자동 적용 (200, 201, 204, 400, 401, 403, 404, 409, 500)
- **병합 규칙**: 기존 주석과 안전하게 병합
- **스캔 범위**: `src/main/java`의 모든 컨트롤러 파일

---

## 5) 생성되는 주석 예시

Cursor LLM이 자동으로 생성하는 주석 예시:

**@Operation 주석**
```java
@Operation(
  summary = "여행 상세 조회",
  description = "여행 ID로 특정 여행의 상세 정보를 조회합니다. 존재하지 않는 여행 ID일 경우 404를 반환합니다.",
  tags = {"여행"}
)
```

**@ApiResponses 주석**
```java
@ApiResponses(value = {
  @ApiResponse(responseCode = "200", description = "여행 조회 성공"),
  @ApiResponse(responseCode = "404", description = "여행을 찾을 수 없음"),
  @ApiResponse(responseCode = "500", description = "서버 오류")
})
```

**@Parameter 주석**
```java
@Parameter(name = "tripId", in = ParameterIn.PATH, required = true, description = "여행 식별자")
```

---

## 6) Cursor LLM 통합 방식

현재는 별도의 프롬프트 파일 없이 Cursor LLM이 직접 파일을 수정합니다:

**동작 방식:**
- 입력: `tools/swagger-agent/out/endpoints.json`
- 출력: 컨트롤러 파일에 직접 Swagger 주석 추가
- 규칙:
  - 한국어 summary/description (간결, 모호어 금지)
  - tag는 컨트롤러명 기반 (MemberController → "Member")
  - 표준 응답 코드 자동 적용 (200, 201, 204, 400, 401, 403, 404, 409, 500)
  - Path/Query/Header/Body에 맞는 @Parameter 생성
  - 기존 주석 존재 시 안전하게 병합

**실행 순서:**
1. `/swg-extract` → `endpoints.json` 생성
2. `/swg-apply` → Cursor LLM이 직접 파일 수정
3. IDE에서 Accept/Reject 선택

---

## 7) Cursor 명령 정의(`.cursor/commands/`)
**현재 구현된 명령어들**:
- `/swg-extract`: 컨트롤러 메타데이터 추출 (자동 실행, 통계 출력)
- `/swg-apply`: Swagger 주석 생성 및 적용 → IDE에서 Accept/Reject 선택 가능

**명령어 동작 방식**:
- **`/swg-extract`**: JavaParser로 컨트롤러 스캔 → `endpoints.json` 생성 → 통계 출력
- **`/swg-apply`**: `endpoints.json` 읽기 → Cursor LLM으로 직접 파일 수정 → IDE에서 변경사항 표시

**명령어 파일 구조**:
```
.cursor/commands/
├─ swg-extract       # 메타데이터 추출 명령 (실행 파일)
├─ swg-extract.md    # 메타데이터 추출 명령 (문서)
└─ swg-apply.md      # 실제 적용 + IDE Accept/Reject 명령
```

---

## 8) TODO 리스트 (처음부터 끝까지)
### 8.1 사전 준비
- [x] JDK 17+ 설치 확인 (`java -version`).
- [x] 파일럿 레포 클론 → 작업 브랜치 생성:  
  `git clone https://github.com/tkddn204/fastcampus-spring-toy-project && cd fastcampus-spring-toy-project && git checkout -b chore/swagger-agent-setup`
- [x] `tools/swagger-agent/` 및 `.cursor/` 스캐폴드 생성.

### 8.2 Extractor 구현
- [x] **데이터 모델 설계**: Controller → Methods → Params/RequestBody/Responses/Exceptions/Existing.
- [x] **AST 파서(JavaParser)**로 다음 수집:
  - 클래스/메소드 매핑(`@RequestMapping`, `@Get/Post/Put/DeleteMapping`)
  - 파라미터 역할(`@PathVariable`, `@RequestParam`, `@RequestHeader`, `@RequestBody`)
  - 검증 애노테이션(`@NotNull`, `@Size` 등)
  - 반환 타입/예외 목록/기존 Swagger 주석 여부
- [x] `endpoints.json` 저장.  
- [x] 스크립트 `scripts/run_extract.sh` 작성(루트에서 호출 가능).

### 8.3 LLM 제안 생성(프롬프트 설계)
- [x] `agent.md`에 규칙·스키마·예시 입출력 명시.
- [x] summary/description 한국어 간결체, 응답 코드 정책(200/201/204/400/401/403/404/409/500).
- [x] 파라미터 설명/예시 자동 추론 룰(id/email/page 등 간단 값).
- [x] 결과를 `suggestions.json`으로 출력하도록 지시.

### 8.4 Cursor LLM 통합 (기존 Injector 대체)
- [x] **전략 선택**: (A) AST 직접 주입(권장), (B) unified diff 생성 후 `git apply`.
- [x] **멱등성**: 기존 주석 확인 후 안전하게 추가/수정
- [x] 병합 규칙:
  - `@Operation` 있으면 summary/description/tags 갱신.
  - `@ApiResponses`는 status 코드 기준 병합.
  - `@Parameter`는 `(name,in)` 키로 덮어쓰기.
- [x] Cursor IDE 통합: Accept/Reject 선택 가능

### 8.5 명령 파이프 구성
- [x] 불필요한 파일 정리: injector 폴더, run_inject.sh, run_all.sh 삭제
- [x] 불필요한 파일 정리: templates 폴더, agent.md, swagger-agent.yml 삭제
- [x] `.cursor/commands/`에서 `/swg-extract`/`/swg-apply` 명령 구현 (실행 파일 + 문서)

### 8.6 파일럿 검증
- [x] `/swg-extract` 실행 → `endpoints.json` 생성 확인.
- [x] `/swg-apply` 실행 → 실제 코드 삽입 (MemberController에 적용됨).
- [x] **명령어 동작 방식 개선**: `/swg-apply`가 실제 파일 수정 후 IDE에서 Accept/Reject 선택 가능하도록 변경
- [x] **LLM 분석 결과 출력**: `/swg-extract`가 터미널에서 간단한 메시지만 출력하고, LLM이 JSON을 읽어서 보기 좋게 정리해서 출력
- [x] **스크립트 간소화**: `run_extract.sh`에서 불필요한 통계 출력 제거
- [x] **문서 정리**: `.cursor/commands/swg-extract.md`에서 중복된 bash 코드 블록 제거
- [ ] 빌드 확인(필요 시 의존성 추가):
  ```kotlin
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
  ```
- [ ] Swagger UI에서 주석 반영 확인(선택).

### 8.7 품질 튜닝
- [ ] summary 길이·어조 일관화(명령형, 40자 내외).
- [ ] description에 검증 실패/권한/에러 언급 1~2문장 포함.
- [ ] 메소드 기반 자동 응답: `POST→201`, `DELETE/PUT(no body)→204`.
- [ ] 예외명 패턴 매핑: `*NotFound*→404`, `*Conflict*→409`.
- [ ] DTO 스키마 추론 실패 시 `Object.class` 폴백 + 경고 로그.

### 8.8 문서화 & 데모
- [ ] `README.md`(설치/실행/예시)와 `docs/DEMO.md`(전/후 비교 스샷) 작성.
- [ ] 데모 스크립트: apply→빌드→Swagger UI 확인 루틴.

---

## 9) 실행 스크립트 샘플 (현재 구현)
**scripts/run_extract.sh**
```bash
#!/usr/bin/env bash
set -e
REPO_ROOT="$(git rev-parse --show-toplevel)"
java -cp tools/swagger-agent/extractor/build/libs/swagger-agent-extractor.jar 
  io.swaggeragent.extractor.Main 
  --src="$REPO_ROOT/src/main/java" 
  --out="$REPO_ROOT/tools/swagger-agent/out/endpoints.json"
```

**Cursor 명령어**
- `/swg-extract`: 자동 실행, 통계 출력
- `/swg-apply`: 직접 파일 수정, IDE Accept/Reject

---

## 10) 트러블슈팅 요약- **주석 중복/충돌**: 마커 블록 기준으로만 덮어쓰기. 수동 주석은 보존.
- **경로 결합 오류**: 클래스/메소드 path 정규화(`//` 제거, trailing 슬래시 정리).
- **제네릭/응답 스키마 인지 실패**: `implementation=Object.class`로 폴백 후 로그.
- **빌드 실패(의존성 없음)**: `springdoc-openapi-starter-webmvc-ui` 추가.
- **CI 도입**: dryrun을 PR 체크에 넣어 주석 누락을 자동 감지.

---

## 11) 오픈소스화 메모(선택)
- 라이선스: MIT 권장(현업 사용 제약 최소).
- 배포: Maven Central(코어/CLI/Gradle/Maven 플러그인은 후속).
- 문서: `README`, `CONFIG`, `EXTENSIONS`, `CONTRIBUTING`, 이슈 템플릿.

---

### 부록 A. 결과 주석 예시
```java
// @generated-by: swagger-agent v0.1 (2025-10-12)
@Operation(
  summary = "사용자 조회",
  description = "경로 변수 id로 사용자를 조회합니다. 유효하지 않은 id일 경우 404를 반환합니다.",
  tags = {"Users"}
)
@ApiResponses(value = {
  @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = UserResponse.class))),
  @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
  @ApiResponse(responseCode = "500", description = "서버 오류")
})
@GetMapping("/{id}")
public ResponseEntity<UserResponse> getUser(
  @Parameter(name="id", in=ParameterIn.PATH, required=true, description="사용자 식별자")
  @PathVariable Long id
) { ... }
```

---

## 12) 현재 구현 상태 (2025-10-12 업데이트)

### ✅ 완료된 기능 (2025-10-12 업데이트)
1. **Extractor**: JavaParser 기반 컨트롤러 메타데이터 추출 완료
2. **LLM Generator**: Cursor 프롬프트 기반 Swagger 주석 제안 생성 완료
3. **Cursor LLM 통합**: 직접 파일 수정 방식으로 구현 완료
4. **Cursor 명령어**: `/swg-extract` (자동 실행), `/swg-apply` (직접 수정) 구현 완료

### 🎯 사용법
```bash
# 1단계: 메타데이터 추출 (자동 실행 + LLM 분석 결과 출력)
/swg-extract

# 2단계: 실제 주석 적용 (IDE에서 Accept/Reject 선택 가능)
/swg-apply
```

### 📊 현재 상태
- **추출된 컨트롤러**: 3개 (MemberController, TripController, ItineraryController)
- **발견된 메소드**: 13개 (모든 컨트롤러 메소드)
- **HTTP 메소드 분포**: GET 6개, POST 5개, PUT 2개
- **파라미터 타입 분포**: @PathVariable 10개, @RequestParam 8개, @RequestBody 6개
- **멱등성**: 기존 주석 확인 후 안전하게 추가/수정
- **IDE 통합**: Cursor에서 Accept/Reject 선택 가능

### 🔧 최근 개선사항 (2025-10-12)
1. **명령어 동작 방식 개선**: `/swg-extract`가 터미널에서 간단한 메시지만 출력하고, LLM이 JSON을 읽어서 보기 좋게 정리해서 출력
2. **개발자 경험 향상**: 복잡한 터미널 출력 대신 LLM이 분석한 결과를 보기 좋게 표시
3. **스크립트 간소화**: `run_extract.sh`에서 불필요한 통계 출력 제거
4. **문서 정리**: `.cursor/commands/swg-extract.md`에서 중복된 bash 코드 블록 제거
5. **불필요한 파일 정리**: templates, agent.md, swagger-agent.yml 삭제로 프로젝트 간소화
6. **워크플로우 최적화**: 추출 → LLM 분석 → 적용의 명확한 단계 분리

---

> **요약**: 이 문서의 순서대로 **폴더 스캐폴드 → Extractor → 프롬프트 → Cursor LLM 통합 → 명령 연결**을 완료했습니다. 파일럿 리포에서 `/swg-extract`와 `/swg-apply` 명령으로 바로 **Swagger 주석 자동 생성 데모**가 가능합니다.
