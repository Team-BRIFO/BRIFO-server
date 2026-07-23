`GET /api/diaries`

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
| cursor | UUID | N | - | 다음 목록 조회 기준 커서 | 직전 응답의 `page.nextCursor` |
| size | Integer | N | 20 | 조회 개수 | 1~50 |

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
    "page": {
      "items": [
      {
        "diaryId": "8fd2c732-b4f1-4b66-b53a-95b5f11df391",
        "stock": {
          "stockId": "0d6d4f4a-7d5d-4e4d-bf71-4b59d18c1a01",
          "name": "삼성전자"
        },
        "decision": {
          "direction": "UP",
          "apDelta": 80,
          "isCorrect": true
        }
      }
      ],
      "nextCursor": "8fd2c732-b4f1-4b66-b53a-95b5f11df391",
      "hasNext": true
    }
  }
}
```

| Field | Type | Description |
| --- | --- | --- |
| page | Object | 결정일기 커서 페이지 |
| page.items | Array | 결정일기 목록 |
| page.nextCursor | UUID \| null | 반환된 마지막 항목의 `diaryId`. 다음 데이터가 없으면 `null` |
| page.hasNext | Boolean | 다음 데이터 존재 여부 |

#### page.items[]

| Field | Type | Description |
| --- | --- | --- |
| diaryId | UUID | 결정일기 공개 ID |
| stock | Object | 주식 정보 |
| decision | Object | 결정 정보 |

#### page.items.stock

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| stockId | UUID | 종목 공개 ID |
| name | String | 종목명 |

#### page.items.decision

| Field | Type | Description |
| --- | --- | --- |
| direction | String | 사용자 결정 방향. `UP`, `DOWN`, `NEUTRAL` |
| apDelta | Integer | 획득 또는 차감 AP. AP 변동이 없으면 0 |
| isCorrect | Boolean | 예측 정답 여부 |

### Error

| HTTP Status | Code | Message |
| --- | --- | --- |
| 400 | COMMON_400 | 잘못된 요청입니다. |
| 401 | AUTH_401 | 인증이 필요합니다. |
| 500 | COMMON_500 | 서버 내부 오류가 발생했습니다. |

### FE Notes

- 정산 완료된 결정일기만 내려온다.
- `apDelta`는 획득이면 양수, 차감이면 음수, AP 변동이 없으면 0이다.

### BE Notes

#### Identifier

- `diaryId`는 `diary_entries.public_id` 기준이다.
- `stockId`는 `stocks.public_id` 기준이다.
- 커서는 `diaryId`를 기준으로 한다.
- `diary_entries.public_id`는 UUIDv7 값이므로 시간순 정렬 가능한 공개 식별자이다.
- 목록은 `diary_entries.public_id DESC`로 정렬한다.
- `cursor`가 있으면 `diary_entries.public_id < cursor` 조건을 적용한다.
- `size + 1`개를 조회해 `page.hasNext`를 판단하고, 다음 데이터가 있으면 반환 항목 중 마지막 항목의 `diaryId`를 `page.nextCursor`로 반환한다.
- 내부 PK `id`는 요청, 응답, cursor에 노출하지 않는다.

#### Schema Mapping

- 목록 기본 경로는 `diary_entries -> decisions -> briefings -> news_cards -> news -> stocks`이다.
- 사용자별 필터는 `decisions.briefing_id -> briefings.agent_id -> agents.user_id`를 기준으로 적용한다.
- `direction`은 `decisions.direction`이다.
- `isCorrect`는 `decision_results.is_correct`이며, 목록은 `decision_results`가 존재하는 데이터만 포함하므로 `null`이 아니다.
- `apDelta`는 `ap_transactions.target_type = 'DECISION'`, `ap_transactions.target_id = decisions.id`인 정산 거래의 `amount`이다.
- 정산된 모든 결정은 AP 처리 거래를 정확히 한 건 생성한다.
- 관망 예측 오답은 `reason = 'NEUTRAL_MISS'`, `amount = 0`인 거래로 기록한다.

#### DTO Candidates

- `stock`은 diary 목록, 상세, 통계에서 `DiaryStockSummary` 후보이다.
- `decision`은 목록 전용 요약 DTO로 둔다.

#### QueryDSL

- 사용자별 결정일기 목록, 종목 조인, AP 거래 계산 필드, cursor pagination이 필요하므로 QueryDSL 후보이다.
