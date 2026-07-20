# Initial Schema Design Notes

이 문서는 Flyway 마이그레이션을 모두 적용한 현재 스키마에서 의도적으로 복잡하게 설계된 식별자, 제약, 인덱스, 참조 구조를 설명한다.
JPA 엔티티 작성 규칙은 `docs/config/jpa-entity-rules.md`에서 다루며, 이 문서는 DB 스키마가 보장하려는 도메인 규칙과 운영 의도에 집중한다.


## Timezone Policy

프로젝트의 시간 기준은 `Asia/Seoul`이다.

일 단위 제약, 출석 보상, 연속근무일, 무한 스크롤 정렬 기준은 모두 `Asia/Seoul` 기준 날짜와 시간 정책에 맞아야 한다.

특히 `TIMESTAMP` 값을 날짜로 변환해 사용하는 제약은 DB 세션 시간대와 애플리케이션 시간대가 달라지면 날짜 경계가 어긋날 수 있다.
운영 환경에서는 DB와 애플리케이션의 시간대 정책을 일관되게 유지해야 한다.


## Internal Id and Public Id

스키마는 내부 식별자와 외부 공개 식별자를 구분한다.

| 식별자 | 용도 |
| --- | --- |
| `id` | DB 내부 PK, FK 관계, 내부 처리 |
| `public_id` | API 응답, 상세 페이지 링크, 외부 요청 파라미터, 커서 |

내부 PK인 `id`는 외부에 노출하지 않는다.
외부에 노출되는 식별자는 `public_id`를 사용한다.

이렇게 나누는 이유는 다음과 같다.

- 내부 PK 노출을 줄여 보안 안정성을 높인다.
- 외부 API가 DB 내부 row 생성 순서나 테이블 구조에 직접 의존하지 않게 한다.
- 외부 공개 식별자 정책을 내부 FK 설계와 분리한다.

단, 모든 테이블에 `public_id`가 필요한 것은 아니다.
외부에 공개될 일이 없거나 내부 로깅 목적으로만 사용하는 테이블은 `public_id`를 두지 않는다.

예를 들어 다음과 같은 테이블은 외부 공개 식별자가 필요하지 않다.

- `attendance_rewards`
- `decision_results`
- `external_api_call_logs`

### Why UUIDv7

`public_id`는 UUIDv7을 사용한다.

UUIDv7은 UUID 안에 시간 정보가 포함되어 있어 시간순 정렬에 유리하다.
이 프로젝트는 무한 스크롤이 많은 편이므로 커서 구성이 중요하다.

커서를 `uuid + created_at` 조합으로 구성할 수도 있지만, UUIDv7을 사용하면 시간 정렬 가능한 공개 식별자 하나를 중심으로 커서를 구성할 수 있어 구현이 단순해진다.


## Soft Delete and Partial Unique Index

프로젝트의 삭제는 기본적으로 soft delete다.

삭제 대상 row는 물리 삭제하지 않고 `deleted_at`을 채운다.
현재는 삭제된 row를 조회하는 API가 없으므로, 일반 조회에서는 삭제된 row를 제외하는 것이 기본 정책이다.

soft delete 대상 테이블에서 일반 `UNIQUE` 제약만 사용하면 삭제된 row 때문에 재가입이나 재등록 같은 정상 흐름이 막힐 수 있다.
그래서 삭제되지 않은 row에 대해서만 unique를 보장하는 partial unique index를 사용한다.

예시:

```sql
CREATE UNIQUE INDEX idx_users_provider_social_id_active_unique
    ON users (provider, social_id)
    WHERE deleted_at IS NULL;
```

```sql
CREATE UNIQUE INDEX idx_user_stocks_active_unique
    ON user_stocks (user_id, stock_id)
    WHERE deleted_at IS NULL;
```

이 구조에서는 삭제된 row와 같은 값을 가진 새 row를 다시 만들 수 있다.
다만 삭제되지 않은 활성 row끼리는 중복될 수 없다.


## News Card, Briefing, and Decision Constraints

뉴스, 카드뉴스, 브리핑, 결정의 전체 기능 흐름은 기능 명세에서 다룬다.
이 문서에서는 FK와 unique 제약이 복잡해 보이는 이유만 설명한다.

핵심 규칙은 다음과 같다.

- 1개 카드뉴스에는 `FAILED`가 아닌 브리핑이 최대 3개 존재할 수 있다.
- 3개 브리핑 중 사용자는 하나를 선택해 1개의 결정을 만든다.
- 결론적으로 사용자는 1개 카드뉴스에 대해 1개의 결정만 만들 수 있다.

### Briefing Uniqueness

`briefings`에는 다음 partial unique index가 있다.

```sql
CREATE UNIQUE INDEX idx_briefings_card_agent_active_unique
    ON briefings (card_id, agent_id)
    WHERE status <> 'FAILED';
```

이 제약은 같은 에이전트가 같은 카드뉴스에 대해 진행 중이거나 완료된 브리핑을 중복 생성하지 못하게 한다.
최종 실패 후 재요청은 기존 row를 변경하지 않고 새 브리핑 row를 생성한다.

카드뉴스당 최대 3개 브리핑이라는 규칙은 이 unique 제약만으로 완전히 표현되지는 않는다.
몇 명의 에이전트가 브리핑을 만들지에 대한 제한은 애플리케이션 로직과 함께 보장한다.

### Decision References

`decisions`는 사용자가 선택한 `briefing_id`만 관계의 진실의 원천으로 가진다.
사용자는 `briefings.agent_id -> agents.user_id`, 카드뉴스는 `briefings.card_id`로 조회한다.

`decisions`에는 다음 제약이 있다.

```sql
briefing_id BIGINT NOT NULL UNIQUE REFERENCES briefings(id) ON DELETE RESTRICT
```

이 제약은 하나의 브리핑이 여러 결정에 재사용되지 않게 한다.

같은 카드뉴스의 다른 브리핑에 이미 결정이 있는지는 Service에서 검증한다.
MVP의 단일 Briefing 상세 선택 UI와 단일 사용자 세션을 전제로 하며, 복수 세션의 서로 다른 Briefing 동시 선택은 DB 제약으로 보장하지 않는다.


## Decision Result

`decisions`는 사용자가 만든 예측을 저장하며 생성 후 변경하지 않는다.
정산 결과는 생명주기가 다른 사실이므로 `decision_results`에 별도 row로 저장한다.

```sql
CREATE TABLE decision_results (
    decision_id          BIGINT PRIMARY KEY REFERENCES decisions(id) ON DELETE RESTRICT,
    daily_stock_price_id BIGINT NOT NULL REFERENCES daily_stock_prices(id) ON DELETE RESTRICT,
    is_correct           BOOLEAN NOT NULL,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW()
);
```

`decision_results` row 존재 여부가 정산 여부의 진실의 원천이다.
`decision_id`가 PK이므로 한 결정은 최대 한 번만 정산된다.
가격, 등락률, 거래일은 `decision_results.daily_stock_price_id`로 연결된 `daily_stock_prices`에서 조회한다.


## AP Transactions as the Source of Truth

`ap_transactions`는 AP 증감 이력을 저장하는 append-only 원장이다.

AP 잔액 변동의 진실의 원천은 `ap_transactions`다.
결정 결과는 `decision_results`, 실제 AP 증감은 `ap_transactions`로 판단한다.
브리핑 일급과 실패 환불은 각각 `SALARY`, `SALARY_REFUND` 거래로 기록하고 브리핑을 직접 참조한다.

### Append-only Policy

`ap_transactions`는 기존 row를 수정하거나 삭제해서 현재 상태를 맞추는 테이블이 아니다.
AP가 증가하거나 감소하는 사건이 발생하면 새 거래 row를 추가한다.

이 방식은 다음 목적에 맞다.

- AP 증감 내역 감사
- 잔액 불일치 추적
- 보상 또는 차감 중복 여부 확인
- 사용자별 AP 히스토리 제공

`users.balance_ap`는 빠른 조회를 위한 현재 잔액 값으로 볼 수 있다.
정합성 검증이 필요할 때는 `ap_transactions`의 거래 내역과 대조할 수 있어야 한다.
신규 사용자의 기본 500 AP도 `INITIAL_GRANT` 거래로 기록하며, 사용자 생성과 잔액 갱신을 같은 트랜잭션에서 처리한다.

### Amount Sign Rules

`NEUTRAL_MISS`는 AP 변동이 없는 정산 사실을 기록하기 위해 `amount = 0`을 사용한다.
그 외 reason의 부호와 0 허용 여부는 V17 방침에 따라 애플리케이션에서 검증한다.

| reason | amount |
| --- | --- |
| `INITIAL_GRANT` | 양수 |
| `ATTENDANCE` | 양수 |
| `TUTORIAL` | 양수 |
| `BADGE` | 양수 |
| `DECISION_WIN` | 양수 |
| `NEUTRAL_HIT` | 양수 |
| `NEUTRAL_MISS` | 0 |
| `CREDIT_LOAN` | 양수 |
| `SALARY_REFUND` | 양수 |
| `DECISION_LOSE` | 음수 |
| `SALARY` | 음수 |

Entity 생성 규칙은 보상 거래가 음수로 들어가거나 차감 거래가 양수로 들어가는 오류를 막는다.

### Reason and Reference Rules

`ap_transactions`는 거래가 왜 발생했는지 기록하기 위해 `reason`, `target_type`, `target_id`를 함께 사용한다.
`target_type`과 `target_id`는 둘 다 없거나 둘 다 있어야 한다.
하나만 존재하는 불완전한 참조는 허용하지 않는다.
이 조합은 DB CHECK가 아니라 Entity 생성 시 검증한다.

또한 `reason`별로 허용되는 `target_type`이 정해져 있다.

| reason | target_type |
| --- | --- |
| `INITIAL_GRANT` | 없음 |
| `ATTENDANCE` | `ATTENDANCE_REWARD` |
| `BADGE` | `USER_BADGE` |
| `DECISION_WIN` | `DECISION` |
| `DECISION_LOSE` | `DECISION` |
| `NEUTRAL_HIT` | `DECISION` |
| `NEUTRAL_MISS` | `DECISION` |
| `SALARY` | `BRIEFING` |
| `SALARY_REFUND` | `BRIEFING` |
| `TUTORIAL` | 없음 |
| `CREDIT_LOAN` | 없음 |

초기 지급, 튜토리얼 보상과 대출은 별도 원인 테이블을 참조하지 않으므로 `target_type`, `target_id`가 없다.

### One-time AP Events

초기 지급, 튜토리얼 보상과 대출은 사용자당 1회만 허용한다.
이를 위해 특정 reason에만 적용되는 partial unique index를 사용한다.

```sql
CREATE UNIQUE INDEX idx_ap_transactions_user_initial_grant_unique
    ON ap_transactions (user_id)
    WHERE reason = 'INITIAL_GRANT';
```

```sql
CREATE UNIQUE INDEX idx_ap_transactions_user_tutorial_unique
    ON ap_transactions (user_id)
    WHERE reason = 'TUTORIAL';
```

```sql
CREATE UNIQUE INDEX idx_ap_transactions_user_credit_loan_unique
    ON ap_transactions (user_id)
    WHERE reason = 'CREDIT_LOAN';
```

이 제약은 다른 AP 거래는 여러 번 허용하면서도, 초기 지급과 튜토리얼 보상, 대출만 사용자당 한 번으로 제한한다.


## Polymorphic Reference Pattern

일부 테이블은 여러 종류의 원인 대상을 참조해야 한다.

이때 원인 테이블마다 FK 컬럼을 만들면 대부분의 컬럼이 `NULL`이 된다.
예를 들어 AP 거래의 원인을 모두 개별 FK로 표현하면 다음과 같은 컬럼이 필요해진다.

- `decision_id`
- `briefing_id`
- `attendance_reward_id`
- `user_badge_id`

하지만 실제 row 하나에서는 이 중 하나만 값이 있고 나머지는 모두 `NULL`이 된다.
이 구조는 컬럼이 불필요하게 늘어나고, row의 의미를 읽기 어렵게 만든다.

그래서 AP 원장은 `ref_type + ref_id`, 알림 이동 대상은 `target_type + target_public_id` 패턴을 사용한다.

### Notifications

`notifications`는 알림 클릭 시 상세 페이지로 이동할 수 있어야 한다.
그래서 내부 PK가 아니라 외부 공개 식별자인 `target_public_id`를 사용한다.

```sql
target_type      VARCHAR(30) NOT NULL,
target_public_id UUID
```

상세 페이지가 있는 알림 타입은 `target_public_id`가 필요하다.
상세 페이지가 없는 알림 타입은 `target_public_id`가 없어야 한다.

V17 방침에 따라 다음 조합은 DB CHECK가 아니라 `Notification` 생성 시 검증한다.

상세 페이지가 없는 타입에는 공개 식별자를 저장하지 않고, 상세 페이지가 필요한 타입에는 공개 식별자를 반드시 전달한다.

### AP Transactions

`ap_transactions`는 내부 원장과 로깅 목적의 테이블이다.
외부 상세 페이지 링크를 만들기 위한 테이블이 아니므로 내부 PK 기반의 `target_id`를 사용한다.

```sql
target_type VARCHAR(30),
target_id   BIGINT
```

실제 FK 제약과 교차 필드 CHECK를 걸지 않고 Entity 생성 시 허용 가능한 참조 타입과 조합을 검증한다.
참조 대상의 존재 여부는 애플리케이션 서비스 로직에서 함께 보장해야 한다.


## Attendance Reward Daily Uniqueness

`attendance_rewards`는 사용자별 출석 보상 지급 내역을 저장한다.

사용자는 하루에 한 번만 출석 보상을 받을 수 있어야 한다.
이를 위해 `created_at`의 날짜값을 사용하는 unique index를 둔다.

```sql
CREATE UNIQUE INDEX idx_attendance_rewards_user_date
    ON attendance_rewards (user_id, (created_at::date));
```

이 인덱스는 같은 사용자에게 같은 날짜의 출석 보상이 두 번 지급되는 것을 막는다.

주의할 점은 `created_at::date`의 날짜 해석이다.
프로젝트의 하루 기준은 `Asia/Seoul`이므로, DB 세션 시간대와 애플리케이션 시간대가 이 정책과 어긋나지 않아야 한다.


## Glossary Terms and Learned Terms

용어 학습 기능은 다음 세 테이블로 구성된다.

| 테이블 | 역할 |
| --- | --- |
| `glossary_terms` | 용어 사전 |
| `news_card_terms` | 카드뉴스에 노출되는 용어 연결 |
| `user_learned_terms` | 사용자가 학습한 용어 기록 |

### Card Terms

`news_card_terms`는 카드뉴스와 용어를 연결한다.

```sql
UNIQUE (card_id, term_id)
```

이 제약은 같은 카드뉴스에 같은 용어가 중복 연결되는 것을 막는다.

```sql
UNIQUE (card_id, display_order)
```

이 제약은 같은 카드뉴스 안에서 용어 표시 순서가 중복되는 것을 막는다.

즉, 하나의 카드뉴스 안에서는 같은 용어가 한 번만 등장하고, 표시 순서도 서로 달라야 한다.

### Learned Terms

`user_learned_terms`는 사용자가 학습한 용어를 기록한다.

```sql
UNIQUE (user_id, term_id)
```

이 제약은 같은 사용자가 같은 용어를 중복 학습 처리하지 못하게 한다.

학습 여부 조회, 학습한 용어 목록, 용어 학습 보상 같은 기능은 이 테이블을 기준으로 판단한다.
