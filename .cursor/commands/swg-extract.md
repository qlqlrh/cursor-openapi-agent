# Swagger Agent - 메타데이터 추출

## 역할
컨트롤러 코드를 분석하여 Swagger 주석 생성을 위한 메타데이터를 추출합니다.

## 동작
1. `src/main/java` 폴더의 모든 컨트롤러 파일을 스캔
2. 각 메소드의 HTTP 메소드, 경로, 파라미터, 반환 타입 등을 분석
3. `cursor-openapi-agent/out/endpoints.json` 파일로 결과 저장
4. 추출된 결과에 대한 통계 정보 출력

## 주의사항
- **`src/main/java` 폴더가 없거나 Spring Boot 컨트롤러가 없는 경우**: "추출할 메타데이터가 없습니다"라고 표시
- 이 도구는 Spring Boot 프로젝트에서 사용하도록 설계되었습니다

## 언제 실행하나요?
- 새로운 컨트롤러 메소드 추가
- 기존 메소드의 파라미터 변경 (`@PathVariable`, `@RequestParam`, `@RequestBody` 등)
- HTTP 메소드나 경로 변경
- 반환 타입 변경
- 새로운 예외 추가

## 실행 결과
- `cursor-openapi-agent/out/endpoints.json` 파일이 생성/업데이트됩니다
- 추출된 엔드포인트 수, 컨트롤러 수, HTTP 메소드별 분포 등 통계 정보 출력
- 이 파일은 다음 단계인 `/swg-apply` 명령어에서 사용됩니다

## 다음 단계
메타데이터 추출이 완료되면:
- `/swg-apply`: 실제 Swagger 주석 적용

---

**자동 실행 중...** `cursor-openapi-agent/scripts/run_extract.sh`를 실행하고 LLM이 결과를 분석하여 보기 좋게 출력합니다.

실제 스크립트는 `cursor-openapi-agent/scripts/run_extract.sh`에서 확인할 수 있습니다.