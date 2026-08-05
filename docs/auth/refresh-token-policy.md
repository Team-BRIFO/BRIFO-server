# Refresh Token 정책

## 결정

- Refresh Token 원문은 서버에 저장하지 않는다.
- 재발급 및 로그아웃에 사용된 Refresh Token의 `jti`, 사용자 식별자, 만료 시각만 PostgreSQL에 저장한다.
- 폐기 기록은 매일 오전 4시(Asia/Seoul)에 정리하며, 이미 만료된 기록만 삭제한다.
- 현재 트래픽과 운영 복잡도를 고려해 Redis는 도입하지 않는다. DB의 고유 제약으로 동시 요청 간 재사용 경쟁도 차단한다.
- Refresh Token은 요청 body와 응답 body로 전달한다. 웹 전용 HttpOnly Cookie로 전환하지 않고, 클라이언트 플랫폼의 보안 저장소를 사용한다.
- 재발급에 성공하면 기존 Refresh Token을 폐기하고 Access Token과 Refresh Token을 모두 교체한다.
- 로그아웃은 Refresh Token만 폐기한다. Access Token은 기본 만료 시간인 1시간까지 유효하며, 모든 인증 요청에 저장소 조회를 추가하는 Access Token 폐기 저장소는 도입하지 않는다.

폐기 기록 정리 주기는 `REFRESH_TOKEN_CLEANUP_CRON` 환경 변수로 변경할 수 있다.

## 클라이언트 계약

1. Access Token과 Refresh Token은 플랫폼의 보안 저장소에 보관한다.
2. Access Token 만료로 여러 요청이 동시에 실패해도 재발급 요청은 하나만 실행한다(single-flight).
3. 진행 중인 재발급 요청이 있으면 다른 요청은 그 결과를 기다린다.
4. 재발급 성공 응답의 Access Token과 Refresh Token을 한 번에 교체한 뒤 대기 중인 요청을 재시도한다.
5. 재발급 요청 자체는 자동 재시도하지 않는다. 같은 Refresh Token을 다시 보내면 재사용으로 차단된다.
6. 재발급 API가 `AUTH_401_04`, `AUTH_401_05`, `AUTH_401_06`을 반환하면 저장된 토큰 쌍을 모두 삭제하고 OAuth 로그인을 다시 시작한다.
7. 로그아웃 시 서버 요청의 성공 여부와 관계없이 로컬 토큰 쌍을 삭제한다.

## 서버 동시성 계약

동일한 Refresh Token으로 재발급 요청이 동시에 들어오면 하나만 성공한다. 나머지 요청은
`AUTH_401_06`을 반환한다. `revoked_refresh_tokens.token_id`의 고유 제약이 조회와 저장 사이의
경쟁 조건에서도 재사용을 차단한다.
