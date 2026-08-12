# BRIFO Server

> **B**riefing **R**oom for **I**nvestment & **F**inancial **O**perations
>
> "모든 투자 결정은 브리포에서 시작된다"

BRIFO는 사용자가 투자 회사의 **사장(CEO)** 이 되어 개성 있는 AI 사원 3명(루키·프로·탱커)을 고용하고, 이들의 분석 보고서를 바탕으로 투자 의사결정을 내리는 **게이미피케이션 기반 AI 투자 교육 플랫폼**입니다.

이 저장소는 BRIFO의 **백엔드 API 서버(Spring Boot)** 입니다. 인증·사용자·사원·뉴스·브리핑·의사결정·정산·통계·일기·뱃지 도메인을 담당하며, 뉴스 요약/사원 분석 등 LLM 작업은 별도의 **AI 서비스([BRIFO-ai](https://github.com/Team-BRIFO/BRIFO-ai), FastAPI)** 로 위임합니다.

## 📖 프로젝트 소개

| 항목 | 내용 |
| ---- | ---- |
| 한 줄 정의 | "나는 사장, AI는 사원. 우리 팀이 분석하고, 내가 결정한다." |
| 분류 | 게이미피케이션 기반 AI 투자 교육 플랫폼 |
| 핵심 컨셉 | 사장(CEO)이 된 사용자가 AI 사원 3명을 고용하고, 분석 보고서로 투자를 결정 |
| 타겟 사용자 | 2030 주식 입문자~중급(1차), 투자 교육에 관심 있는 대학생/사회초년생(2차) |
| 개발 기간 | 2026.06.22 ~ 2026.08.21 (약 9주) |

### 핵심 게임 루프

```
[출근 / 카드뉴스 확인]
        ↓
[사원에게 분석 업무 할당]
        ↓
[AI 사원 보고서 열람 (3명)]
        ↓
[방향 + 확신도 선택]
        ↓
[15:30 장 마감 자동 정산]
        ↓
[AP 획득 + 사원 EXP 증가]
        ↓
[결정 일기 자동 기록]
        ↓
[다음 날 다시 출근]
```

> 자세한 기획·API·배치·DB 스펙은 팀 Notion과 기능명세서(BRIFO_MVP_기능명세서)를 기준으로 합니다.

---

## 👥 팀원 및 백엔드 역할 분담

| 이름     | GitHub | 담당 역할                                                                           |
|--------| ------ |---------------------------------------------------------------------------------|
| 하나/권민정 | [mjttong](https://github.com/mjttong) | 백엔드 아키텍처 및 핵심 도메인 API 개발, 브리핑·결정일기·배치 시스템 구현, CI/CD 및 배포 환경 구축, Spring batch 작업 |
| 리아/강나현 | [nahyeon647](https://github.com/nahyeon647) | 외부 API 공통 모듈 및 안정성 정책 설계, KIS 주가 조회·캐싱 구현, AI 카드뉴스·브리핑 연동                       |
| 망고/엄민서 | [seo1v](https://github.com/seo1v) | OAuth·JWT·Refresh Token 인증 체계 구현, 사용자·온보딩·홈 API 개발, 결정일기 공유 이미지 생성              |

---

## 🛠 기술 스택

| 분류 | 기술 |
| ---- | ---- |
| 언어 / 런타임 | `Kotlin 2.2.21`, `JDK 21` (toolchain) |
| 프레임워크 | `Spring Boot 4.0.7` (Web MVC) |
| 빌드 | `Gradle` (Kotlin DSL) + `spring-dependency-management` |
| DB / ORM | `PostgreSQL`, `Spring Data JPA` (Hibernate) |
| 마이그레이션 | `Flyway` (`flyway-database-postgresql`) |
| 배치 | `Spring Batch` (+ `batch-jdbc`) |
| 보안 | `Spring Security` (인증/인가) |
| 검증 | `Spring Validation` (Bean Validation) |
| 문서화 | `springdoc-openapi` (Swagger UI) |
| 직렬화 | `Jackson` + `jackson-module-kotlin` |
| 모니터링 | `Spring Boot Actuator` |
| 포맷팅 | `Spotless` + `ktlint 1.8.0` |
| 테스트 | `JUnit 5`, `kotlin-test`, `Testcontainers` (PostgreSQL) |
| 로컬 인프라 | `docker-compose` (PostgreSQL 등) |

> LLM 호출 자체는 서버가 직접 하지 않고 AI 서비스(FastAPI)에 위임합니다. 서버는 요약/브리핑 요청을 트리거하고 결과(JSON)를 영속화·캐싱하는 오케스트레이션을 담당합니다.

---

## 📁 패키지 구조

```
src/main/kotlin/com/brifo/server/
├── ServerApplication.kt
├── global/                 # 공통 설정·인프라
│   ├── code/               # BaseCode, ErrorCode, SuccessCode
│   ├── common/             # ApiResponse, BaseEntity
│   ├── config/             # Security, Jpa, Swagger, Cors 설정
│   ├── error/              # GlobalExceptionHandler, FieldErrorResponse
│   ├── exception/          # BusinessException
│   └── log/                # 외부 API 호출 로그(entity/repository)
└── <도메인>/               # 도메인마다 controller · dto · entity · repository · service
    ├── auth/               # 소셜 로그인/인증
    ├── user/               # 사용자(CEO)
    ├── policy/             # 약관/동의(Policy, UserPolicy)
    ├── agent/              # 사원(루키/프로/탱커), 급여 로그
    ├── stock/              # 종목, 사용자 종목, 일별 시세
    ├── news/               # 뉴스/카드뉴스
    ├── externalapi/        # 외부 API (KIS, NAVER, DART, KRX)
    ├── term/               # 주식 용어(glossary), 학습 기록
    ├── briefing/           # 사원 브리핑
    ├── decision/           # 예측/의사결정
    ├── diary/              # 결정 일기
    ├── ap/                 # AP 원장, 출석 보상
    ├── badge/              # 뱃지/업적
    └── notification/       # 인앱 알림
```

- **레이어 규칙**: `Controller → Service → Repository` 단방향. Controller는 DTO만 다루고, Entity는 도메인 밖으로 노출하지 않습니다.
- **스키마 변경**: `src/main/resources/db/migration/V{n}__{설명}.sql` Flyway 마이그레이션으로 관리합니다. 이미 머지된 마이그레이션 파일은 수정하지 않고 새 버전을 추가합니다.
- 상세 컨벤션은 저장소 내 [`docs/config/convention.md`](docs/config/convention.md), JPA 엔티티 규칙은 [`docs/config/jpa-entity-rules.md`](docs/config/jpa-entity-rules.md)를 따릅니다.

---

## 🌿 컨벤션

- **브랜치**: Git Flow — `main`(운영) / `dev`(개발 통합), 작업 브랜치는 `dev`에서 분기해 `dev`로 병합. 브랜치명 `{type}/{issue-number}-{summary}`.
- **이슈**: 제목 `[Type] 제목` (`Feat` / `Fix` / `Refactor` / `Docs` / `Chore` / `Test` / `CI`).
- **커밋**: `{type}: {subject}` — 타입 소문자, 제목 50자 이내·마침표 없음. (`feat`/`fix`/`refactor`/`docs`/`chore`/`test`/`build`/`ci`/`perf`/`revert`)
- **PR**: 제목 `[Type] 구현_내용`, `.github/PULL_REQUEST_TEMPLATE.md` 사용, 리뷰어·라벨 설정 후 리뷰.

> 전체 규칙(표 포함)은 [`docs/config/convention.md`](docs/config/convention.md)가 기준입니다.

---

## 🚀 실행 방법

```bash
# 1. 저장소 클론
git clone https://github.com/Team-BRIFO/BRIFO-server.git
cd BRIFO-server

# 2. 로컬 인프라 기동 (PostgreSQL 등)
docker compose up -d

# 3. 환경 변수 / 프로파일 설정 (application-{dev|test|prod}.yaml, 시크릿은 커밋 금지)

# 4. 개발 서버 실행 (dev 프로파일 예시)
./gradlew bootRun --args='--spring.profiles.active=dev'

# 5. 빌드 (테스트 포함)
./gradlew build

# 6. 테스트만 실행
./gradlew test

# 7. 코드 포맷 (Spotless + ktlint)
./gradlew spotlessApply     # 자동 정리
./gradlew spotlessCheck     # 검사만
```

- 실행 후 API 문서: `http://localhost:8080/swagger-ui.html`
- 헬스 체크: `http://localhost:8080/actuator/health`
- 민감 정보(DB 접속, JWT 시크릿, OAuth/KIS 키, AI 서비스 URL)는 커밋 금지 — 환경 변수 또는 `application-{profile}.yaml`(gitignore)로 주입합니다.
