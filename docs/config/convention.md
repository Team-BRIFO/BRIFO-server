## Branch Convention

- 브랜치 전략은 Git Flow를 사용
- `main`: 운영 배포용 브랜치
- `dev`: 개발 통합 브랜치
- 작업 브랜치는 `dev`에서 생성하고, 작업 완료 후 `dev`로 병합
- 브랜치명: `{type}/{issue-number}-{summary}`

## Issue Convention

이슈 제목: `[Type] 제목`

| 타입 | 설명 |
| --- | --- |
| `Feat` | 새로운 기능을 추가합니다. |
| `Fix` | 버그를 수정합니다. |
| `Refactor` | 기능 변경 없이 코드를 개선합니다. |
| `Docs` | 문서를 추가하거나 수정합니다. |
| `Chore` | 설정, 빌드, 패키지 등 기능 외 작업을 처리합니다. |
| `Test` | 테스트 코드를 추가하거나 수정합니다. |
| `CI` | CI/CD 설정을 추가하거나 수정합니다. |

## Commit Message Convention

커밋 메시지: `{type}: {subject}`

| 타입 | 설명 |
| --- | --- |
| `feat` | 새로운 기능을 추가합니다. |
| `fix` | 버그를 수정합니다. |
| `refactor` | 기능 변경 없이 코드를 개선합니다. |
| `docs` | 문서를 추가하거나 수정합니다. |
| `chore` | 설정, 빌드, 패키지 등 기능 외 작업을 처리합니다. |
| `test` | 테스트 코드를 추가하거나 수정합니다. |
| `build` | 빌드 시스템 또는 의존성을 변경합니다. |
| `ci` | CI/CD 설정을 추가하거나 수정합니다. |
| `perf` | 성능을 개선합니다. |
| `revert` | 이전 커밋을 되돌립니다. |

작성 규칙:

- 커밋 타입은 소문자
- 제목은 50자 이내
- 제목 끝에는 마침표를 사용하지 않음
- 본문은 필요한 경우에만 작성

## Code Convention

- class/interface name: `UpperCamelCase`
- function/value name: `lowerCamelCase`
- package: 소문자
- enum class name: `UpperCamelCase`
- enum 상수: `UPPER_SNAKE_CASE`
- 클래스 파일명은 클래스명과 동일하게 작성
- 기타 파일명은 관례를 따름
