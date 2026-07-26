`GET /api/diaries/stats`

## Request

### Headers

```json
{
  "Authorization": "Bearer <JWT_ACCESS_TOKEN>"
}
```

### Request Body

없음

### Query Parameters

| Name | Type | Required | Description | Constraints |
| --- | --- | --- | --- | --- |
| userId | UUID | Y | 개발용 인증 사용자 공개 ID | UUID 형식 |

---

## Response

### Success (200 OK)

```json
{
  "success": true,
  "code": "COMMON_200",
  "message": "요청에 성공했습니다.",
  "result": {
    "summary": {
      "recent30DaysSettledDecisionCount": 18,
      "recent30DaysCorrectDecisionCount": 12,
      "recent30DaysAccuracyRate": 66,
      "settledDecisionCount": 52,
      "correctDecisionCount": 31,
      "averageConfidenceLevel": 3.6,
      "bestCorrectStreak": 5
    },
    "directionStats": [
      {
        "direction": "UP",
        "settledDecisionCount": 22,
        "correctDecisionCount": 14,
        "accuracyRate": 63
      },
      {
        "direction": "NEUTRAL",
        "settledDecisionCount": 12,
        "correctDecisionCount": 8,
        "accuracyRate": 66
      },
      {
        "direction": "DOWN",
        "settledDecisionCount": 18,
        "correctDecisionCount": 9,
        "accuracyRate": 50
      }
    ],
    "agentStats": [
      {
        "agentId": "0f2b7e7c-7d8a-4f4f-9b8e-0d1f3a2b9c11",
        "agentType": "ROOKIE",
        "nickname": "루키",
        "settledDecisionCount": 16,
        "correctDecisionCount": 9,
        "accuracyRate": 56
      },
      {
        "agentId": "2d9c6a71-3c5c-46e7-b8a2-54f5c1f7d912",
        "agentType": "PRO",
        "nickname": "프로",
        "settledDecisionCount": 21,
        "correctDecisionCount": 14,
        "accuracyRate": 66
      },
      {
        "agentId": "7a1e6d33-890e-4f73-a18c-9d11c8e7f444",
        "agentType": "TANKER",
        "nickname": "탱커",
        "settledDecisionCount": 15,
        "correctDecisionCount": 8,
        "accuracyRate": 53
      }
    ],
    "confidenceLevelStats": [
      {
        "level": "LOW",
        "settledDecisionCount": 13,
        "correctDecisionCount": 6,
        "accuracyRate": 46
      },
      {
        "level": "MEDIUM",
        "settledDecisionCount": 17,
        "correctDecisionCount": 10,
        "accuracyRate": 58
      },
      {
        "level": "HIGH",
        "settledDecisionCount": 22,
        "correctDecisionCount": 15,
        "accuracyRate": 68
      }
    ],
    "stockStats": [
      {
        "stockId": "9a44f49e-d531-4b9f-93dd-f78f8d5c2c81",
        "name": "삼성전자",
        "settledDecisionCount": 20,
        "correctDecisionCount": 13,
        "accuracyRate": 65
      },
      {
        "stockId": "0a827caf-5c8c-4c2f-8cc3-8a4af8631e4f",
        "name": "NAVER",
        "settledDecisionCount": 14,
        "correctDecisionCount": 8,
        "accuracyRate": 57
      },
      {
        "stockId": "ee5f7320-7acb-4f12-8427-e1f24ef10d21",
        "name": "SK하이닉스",
        "settledDecisionCount": 18,
        "correctDecisionCount": 10,
        "accuracyRate": 55
      }
    ]
  }
}
```

| Field | Type | Description |
| --- | --- | --- |
| summary | Object | 전체 요약 통계 |
| directionStats | Array | 방향별 적중률 |
| agentStats | Array | 사원별 채택 적중률 |
| confidenceLevelStats | Array | 사용자 확신도 단계별 적중률 |
| stockStats | Array | 종목별 적중률 상위 3개 |

### summary

| Field | Type | Description |
| --- | --- | --- |
| recent30DaysSettledDecisionCount | Integer | 최근 30일 정산 완료 결정 수 |
| recent30DaysCorrectDecisionCount | Integer | 최근 30일 적중 결정 수 |
| recent30DaysAccuracyRate | Integer | 최근 30일 적중률. 최근 30일 적중 수를 정산 완료 수로 나누며, 정산 완료 결정이 없으면 0 |
| settledDecisionCount | Integer | 전체 정산 완료 결정 수 |
| correctDecisionCount | Integer | 전체 적중 결정 수 |
| averageConfidenceLevel | Number | 전체 사용자 확신도 단계 평균. `decisions.confidence_level` 평균, 소수점 한 자리 |
| bestCorrectStreak | Integer | 최고 연속 정답 수 |

### directionStats[]

| Field | Type | Description |
| --- | --- | --- |
| direction | String | 결정 방향. `UP`, `NEUTRAL`, `DOWN` |
| settledDecisionCount | Integer | 해당 방향의 전체 정산 완료 결정 수 |
| correctDecisionCount | Integer | 해당 방향의 전체 적중 결정 수 |
| accuracyRate | Integer | 해당 방향의 적중률. 결정이 없으면 0, 소수점 첫째 자리에서 반올림 |

### agentStats[]

| Field | Type | Description |
| --- | --- | --- |
| agentId | UUID | 사원 ID |
| agentType | String | 사원 타입. `ROOKIE`, `PRO`, `TANKER` |
| nickname | String | 사원 이름 |
| settledDecisionCount | Integer | 해당 사원 브리핑을 채택한 전체 정산 완료 결정 수 |
| correctDecisionCount | Integer | 해당 사원 브리핑을 채택한 전체 적중 결정 수 |
| accuracyRate | Integer | 해당 사원 브리핑 채택 적중률. 결정이 없으면 0, 소수점 첫째 자리에서 반올림 |

### confidenceLevelStats[]

| Field | Type | Description |
| --- | --- | --- |
| level | String | 확신도 구간. `LOW`, `MEDIUM`, `HIGH` |
| settledDecisionCount | Integer | 해당 확신도 구간의 전체 정산 완료 결정 수 |
| correctDecisionCount | Integer | 해당 확신도 구간의 전체 적중 결정 수 |
| accuracyRate | Integer | 해당 확신도 구간의 적중률. 결정이 없으면 0, 소수점 첫째 자리에서 반올림 |

### stockStats[]

| Field | Type | Description |
| --- | --- | --- |
| stockId | UUID | 종목 공개 ID |
| name | String | 종목명 |
| settledDecisionCount | Integer | 해당 종목의 전체 정산 완료 결정 수 |
| correctDecisionCount | Integer | 해당 종목의 전체 적중 결정 수 |
| accuracyRate | Integer | 해당 종목의 적중률. 결정이 없으면 0, 소수점 첫째 자리에서 반올림 |

### Error

| HTTP Status | Code | Message |
| --- | --- | --- |
| 401 | AUTH_401 | 인증이 필요합니다. |
| 500 | COMMON_500 | 서버 내부 오류가 발생했습니다. |

### FE Notes

- 모든 적중률은 서버가 정수 퍼센트로 계산해 내려준다.
- 집계 대상이 없으면 count 필드는 0, rate 필드는 0, 배열 필드는 빈 배열이다.
- `stockStats`는 종목별 적중률 상위 3개만 표시한다.

### BE Notes

#### Date Range

- `recent30Days...`는 `Asia/Seoul` 기준 오늘을 포함한 최근 30일이다.
- 기준 범위는 오늘 날짜에서 29일을 뺀 날짜의 `00:00:00` 이상, 내일 `00:00:00` 미만이다.
- 기간 조건은 정답 판정 시각인 `decision_results.created_at`을 기준으로 적용한다.

#### Schema Mapping

- 통계 기본 경로는 `diary_entries -> decisions -> briefings -> agents`와 `decisions -> briefings -> news_cards -> news -> stocks`이다.
- 사용자별 필터는 `decisions.briefing_id -> briefings.agent_id -> agents.user_id`를 기준으로 적용한다.
- 모든 통계는 인증 사용자 본인의 `decision_results`가 존재하는 정산 완료 결정일기만 집계한다.
- `settledDecisionCount` 계열은 `decision_results`가 존재하는 정산 완료 결정 수이다.
- `correctDecisionCount` 계열은 `decision_results.is_correct = true`인 결정 수이다.
- `directionStats.direction`은 사용자 실제 결정인 `decisions.direction` 기준이다.
- `agentStats`는 사용자가 채택한 브리핑의 `briefings.agent_id -> agents` 기준이다.
- `confidenceLevelStats.level`은 `decisions.confidence_level` 기준이며 `LOW = 1~2`, `MEDIUM = 3`, `HIGH = 4~5`이다.
- `stockStats`는 `decisions -> briefings -> briefing_news_cards -> news_cards -> news -> stocks` 기준이다.
- `stockStats`는 `accuracyRate` 내림차순, `settledDecisionCount` 내림차순, `name` 오름차순으로 상위 3개를 반환한다.

#### Calculated Fields

- 적중률은 `correctDecisionCount / settledDecisionCount * 100`을 소수점 첫째 자리에서 반올림한 정수이며, `settledDecisionCount = 0`이면 0이다.
- `averageConfidenceLevel`은 정산 완료 결정의 `decisions.confidence_level` 평균을 소수점 한 자리로 반올림하며, 결정이 없으면 0.0이다.
- `bestCorrectStreak`는 정산 완료 결정일기를 `decision_results.created_at ASC`, `diary_entries.public_id ASC` 순서로 정렬했을 때 `decision_results.is_correct = true`가 연속된 최대 길이이다.

#### DTO Candidates

- `directionStats`, `agentStats`, `confidenceLevelStats`, `stockStats`는 각각 통계 화면 전용 DTO로 둔다.
- `stockStats`의 `stockId`, `name`은 diary 공통 종목 요약 DTO 후보이다.

#### QueryDSL

- 여러 테이블 조인, 그룹별 집계, 최근 30일 조건, 최고 연속 정답 계산이 필요하므로 QueryDSL 후보이다.
