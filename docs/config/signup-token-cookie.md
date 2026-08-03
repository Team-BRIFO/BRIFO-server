# Signup Token Cookie

온보딩 전용 `signupToken`은 로그인 응답 body가 아니라 서버가 설정하는 HttpOnly 쿠키로 전달한다.

## 쿠키 정책

- 이름: `signup_token`
- 경로: `/api`
- 만료 시간: JWT의 `app.auth.jwt.signup-token-expiration`과 동일
- `HttpOnly`: 항상 활성화
- `SameSite`: `Lax`
- `Secure`: 개발 환경에서는 비활성화하고 운영 환경에서는 활성화
- CSRF 쿠키: `signup_csrf_token` (프론트엔드에서 읽어 요청 헤더로 전달)
- CSRF 헤더: `X-Signup-CSRF-Token`

`SameSite=Lax` 정책과 CSRF 쿠키-헤더 일치 검증을 함께 적용한다.

## 프론트엔드 연동

로그인과 온보딩 API 요청에 credential 옵션을 포함한다.

```ts
fetch(url, {
  credentials: "include",
  headers: {
    "X-Signup-CSRF-Token": getCookie("signup_csrf_token"),
  },
});
```

CSRF 헤더는 signup token으로 인증하는 상태 변경 온보딩 요청에 포함한다. `getCookie`는 프론트엔드의 쿠키 조회 유틸리티를 사용한다.

온보딩이 필요한 로그인 응답에는 `loginType: "SIGNUP_REQUIRED"`만 포함되며 `signupToken` 필드는 반환하지 않는다. 프론트엔드는 토큰을 Web Storage에 저장하거나 `Authorization` 헤더로 전달하지 않는다.

온보딩 완료 또는 유효하지 않은 signup token 감지 시 서버가 쿠키를 만료시켜 제거한다.
