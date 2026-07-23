`GET /api/diaries/{diaryId}`

## Request

### Headers

```json
{
  "Authorization": "Bearer <JWT_ACCESS_TOKEN>"
}
```

### Path Parameters

| Name | Type | Description | Constraints |
| --- | --- | --- | --- |
| diaryId | UUID | 결정일기 ID | UUID 형식 |

### Query Parameters

| Name | Type | Required | Description | Constraints |
| --- | --- | --- | --- | --- |
| userId | UUID | Y | 개발용 인증 사용자 공개 ID | UUID 형식 |

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
    "diaryId": "8fd2c732-b4f1-4b66-b53a-95b5f11df391",
    "shareImageUrl": "https://cdn.example.com/share/8fd2c732-b4f1-4b66-b53a-95b5f11df391.png",
    "stock": {
      "stockId": "0d6d4f4a-7d5d-4e4d-bf71-4b59d18c1a01",
      "name": "삼성전자",
      "changeRate": 2.0
    },
    "agent": {
      "agentId": "0f2b7e7c-7d8a-4f4f-9b8e-0d1f3a2b9c11",
      "agentType": "ROOKIE",
      "nickname": "루키"
    },
    "briefing": {
      "briefingId": "5f64e4df-83e5-4050-94f9-a42e0f7f9f1a",
      "direction": "UP",
      "confidenceRate": 72
    },
    "decision": {
      "isCorrect": true,
      "confidenceLevel": 4
    }
  }
}
```

| Field | Type | Description |
| --- | --- | --- |
| diaryId | UUID | 결정일기 공개 ID |
| shareImageUrl | String? | 공유 이미지 URL. 생성 전이면 `null` |
| stock | Object | 종목 및 가격 요약 |
| agent | Object | AI 사원 정보 |
| briefing | Object | AI 사원 의견 |
| decision | Object | 사용자 결정 정보 |

#### stock

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| stockId | UUID | 종목 공개 ID |
| name | String | 종목명 |
| changeRate | Integer | 정답 여부 판정에 사용된 등락률. 소수점 첫째 자리에서 반올림 |

#### agent

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| agentId | UUID | 에이전트 ID |
| agentType | String | 에이전트 종류 |
| nickname | String | 에이전트 이름 |

#### briefing

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| briefingId | UUID | 브리핑 ID |
| direction | String | AI 예측 방향. `UP`, `DOWN`, `NEUTRAL` |
| confidenceRate | Integer | AI 브리핑 확신 비율. 0~100 정수이며 `72`는 72%를 의미 |

#### decision

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| isCorrect | Boolean | 정답 여부 |
| confidenceLevel | Integer | 사용자 확신도 단계. 1~5 |

### Error

| HTTP Status | Code | Message |
| --- | --- | --- |
| 401 | AUTH_401 | 인증이 필요합니다. |
| 404 | DIARY_404 | 결정일기를 찾을 수 없습니다. |
| 500 | COMMON_500 | 서버 내부 오류가 발생했습니다. |

### FE Notes

- 인증 사용자 본인의 결정일기만 조회한다.
- 정산 완료된 결정일기만 조회한다.
- 공유 이미지 생성 전이면 `shareImageUrl`은 `null`이다.

### BE Notes

#### Identifier

- `diaryId`는 `diary_entries.public_id` 기준이다.
- `stockId`는 `stocks.public_id` 기준이다.
- `agentId`는 `agents.public_id` 기준이다.
- `briefingId`는 `briefings.public_id` 기준이다.
- 내부 PK `id`는 요청과 응답에 노출하지 않는다.

#### Access

- 다른 사용자의 결정일기를 조회한 경우에도 보안을 위해 `DIARY_404`를 반환한다.
- 결정일기 소유 사용자는 `diary_entries.decision_id -> decisions.briefing_id -> briefings.agent_id -> agents.user_id`로 판별한다.

#### Schema Mapping

- 상세 기본 경로는 `diary_entries -> decisions -> briefings -> agents`, `decisions -> briefings -> news_cards -> news -> stocks`, `decisions -> decision_results -> daily_stock_prices`이다.
- `shareImageUrl`은 `diary_entries.share_image_url`이다.
- `stock.changeRate`는 정답 여부 판정 시 `decision_results.daily_stock_price_id`로 참조한 `daily_stock_prices.change_rate`이다.
- `briefing.direction`은 `briefings.direction`이다.
- `briefing.confidenceRate`는 `briefings.confidence_rate`이다.
- `decision.isCorrect`는 `decision_results.is_correct`이며, 상세는 `decision_results`가 존재하는 데이터만 조회하므로 `null`이 아니다.
- `decision.confidenceLevel`은 `decisions.confidence_level`이다.

#### DTO Candidates

- `stock`은 diary 목록, 상세, 통계에서 `DiaryStockSummary` 후보이나 상세의 `changeRate`는 상세 전용 확장 필드이다.
- `agent`는 agent 요약 DTO 후보이다.
- `briefing`, `decision`은 상세 전용 DTO로 둔다.

#### QueryDSL

- 결정일기, 결정, 브리핑, 사원, 종목, 정답 판정에 사용된 주가 조인이 필요하므로 QueryDSL 후보이다.
