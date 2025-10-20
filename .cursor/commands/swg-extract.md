# Swagger Agent - 메타데이터 추출

## 역할
컨트롤러 코드를 분석하여 Swagger 주석 생성을 위한 메타데이터를 추출합니다.

## 동작
1. **전체 스캔 모드** (`/swg-extract`): `src/main/java` 폴더의 모든 컨트롤러 파일을 스캔
2. **선택적 스캔 모드** (`/swg-extract @파일명`): 지정된 파일만 스캔 (컨트롤러 + 연관 DTO)
3. 각 메소드의 HTTP 메소드, 경로, 파라미터, 반환 타입 등을 분석
4. DTO 클래스 감지 및 필드 정보 추출 (파일명 패턴: `*Dto`, `*DTO`, `*Request`, `*Response`)
5. `cursor-openapi-agent/out/endpoints.json` 파일로 결과 저장
6. 추출된 결과에 대한 통계 정보 출력

## 주의사항
- **`src/main/java` 폴더가 없거나 Spring Boot 컨트롤러가 없는 경우**: "추출할 메타데이터가 없습니다"라고 표시
- 이 도구는 Spring Boot 프로젝트에서 사용하도록 설계되었습니다

## 사용법
- **전체 스캔**: `/swg-extract` - 모든 컨트롤러 파일 스캔
- **선택적 스캔**: `/swg-extract @파일명` - 특정 파일만 스캔
  - `/swg-extract @UserController.java` - 컨트롤러 파일 (연관 DTO까지 자동 감지)
  - `/swg-extract @UserLoginRequest.java` - DTO 파일만 스캔
  - `/swg-extract @UserDto.java` - DTO 파일만 스캔

## 언제 실행하나요?
- 새로운 컨트롤러 메소드 추가
- 기존 메소드의 파라미터 변경 (`@PathVariable`, `@RequestParam`, `@RequestBody` 등)
- HTTP 메소드나 경로 변경
- 반환 타입 변경
- 새로운 예외 추가
- 새로운 DTO 클래스 추가 (파일명 패턴: `*Dto`, `*DTO`, `*Request`, `*Response`)

## 실행 결과
- `cursor-openapi-agent/out/endpoints.json` 파일이 생성/업데이트됩니다
- 추출된 엔드포인트 수, 컨트롤러 수, DTO 클래스 수, HTTP 메소드별 분포 등 통계 정보 출력
- **DTO 정보 포함**: 파일명 패턴으로 감지된 DTO 클래스와 필드 정보
- **연관 DTO 자동 감지**: 컨트롤러에서 사용되는 `@RequestBody`, 반환 타입의 DTO 클래스
- 이 파일은 다음 단계인 `/swg-apply` 명령어에서 사용됩니다

## 다음 단계
메타데이터 추출이 완료되면:
- `/swg-apply`: 실제 Swagger 주석 적용

---

**자동 실행 중...** `cursor-openapi-agent/scripts/run_extract.sh`를 실행하고 LLM이 결과를 분석하여 보기 좋게 출력합니다.

- **전체 스캔**: `run_extract.sh` (기본 동작)
- **선택적 스캔**: `run_extract.sh --files 파일경로1 파일경로2 ...` (선택된 파일들만 처리)

실제 스크립트는 `cursor-openapi-agent/scripts/run_extract.sh`에서 확인할 수 있습니다.