`GET /api/diaries/calendar`

## Request

### Headers

```json
{
  "Authorization": "Bearer <JWT_ACCESS_TOKEN>"
}
```

### Query Parameters

| Name | Type | Required | Default | Description | Constraints |
| --- | --- | --- | --- | --- | --- |
| userId | UUID | Y | - | 개발용 인증 사용자 공개 ID | UUID 형식 |
| year | Integer | Y | - | 조회 연도 | 2000 이상 |
| month | Integer | Y | - | 조회 월 | 1~12 |

### Request Body

없음

---

## Response

### Success (200 OK)

```json
{
  "success": true,
  "code": "COMMON_200",
  "message": "요청에 성공했습니다.",
  "result": {
    "year": 2026,
    "month": 7,
    "settledDecisionCount": 21,
    "correctDecisionCount": 12,
    "accuracyRate": 57,
    "days": [
      {
        "date": "2026-07-01",
        "direction": {
          "up": true,
          "down": true,
          "neutral": false
        }
      },
      {
        "date": "2026-07-02",
        "direction": {
          "up": false,
          "down": false,
          "neutral": true
        }
      }
    ]
  }
}
```

| Field | Type | Description |
| --- | --- | --- |
| year | Integer | 조회 연도 |
| month | Integer | 조회 월 |
| settledDecisionCount | Integer | 월 정산 완료 결정 수 |
| correctDecisionCount | Integer | 월 적중 결정 수 |
| accuracyRate | Integer | 월 적중률. `correctDecisionCount / settledDecisionCount * 100`, 정산 완료 결정이 없으면 0, 소수점 첫째 자리에서 반올림 |
| days | Array | 정산 완료 결정이 있는 날짜 목록 |

#### days

| Field | Type | Description |
| --- | --- | --- |
| date | String | 날짜, `YYYY-MM-DD` |
| direction | Object | 날짜별 결정 |

#### days.direction

| Field | Type | Description |
| --- | --- | --- |
| up | Boolean | 해당 날짜에 `decisions.direction = UP` 결정이 있는지 여부 |
| down | Boolean | 해당 날짜에 `decisions.direction = DOWN` 결정이 있는지 여부 |
| neutral | Boolean | 해당 날짜에 `decisions.direction = NEUTRAL` 결정이 있는지 여부 |

### Error

| HTTP Status | Code | Message |
| --- | --- | --- |
| 400 | COMMON_400 | 잘못된 요청입니다. |
| 401 | AUTH_401 | 인증이 필요합니다. |
| 500 | COMMON_500 | 서버 내부 오류가 발생했습니다. |

### FE Notes

- `days`에는 정산 완료 결정이 있는 날짜만 포함한다.
- 방향별 값은 해당 날짜에 같은 방향 결정이 1건 이상 있으면 `true`이다.
- 월 정산 완료 결정이 없으면 `settledDecisionCount`, `correctDecisionCount`, `accuracyRate`는 모두 0이고 `days`는 빈 배열이다.

### BE Notes

#### Date Range

- 조회 기간은 `Asia/Seoul` 기준 `year-month-01 00:00:00` 이상, 다음 달 1일 `00:00:00` 미만이다.
- 날짜 그룹핑은 `decisions.created_at`을 `Asia/Seoul` 기준 날짜로 변환해 계산한다.

#### Schema Mapping

- 캘린더 기본 경로는 `diary_entries -> decisions`이다.
- 사용자별 필터는 `decisions.briefing_id -> briefings.agent_id -> agents.user_id`를 기준으로 적용한다.
- `decision_results`가 존재하는 정산 완료 결정일기만 집계한다.
- `settledDecisionCount`는 월 정산 완료 결정 수이다.
- `correctDecisionCount`는 월 정산 결정 중 `decision_results.is_correct = true`인 결정 수이다.
- `accuracyRate`는 `correctDecisionCount / settledDecisionCount * 100`을 소수점 첫째 자리에서 반올림한 정수이며, `settledDecisionCount = 0`이면 0이다.
- `days.direction`은 사용자 실제 결정인 `decisions.direction` 기준이다.

#### DTO Candidates

- 캘린더 응답은 화면 전용 집계 DTO로 둔다.

#### QueryDSL

- 월 범위 조건, 사용자별 필터, 날짜별 그룹핑, 방향별 집계가 필요하므로 QueryDSL 후보이다.
