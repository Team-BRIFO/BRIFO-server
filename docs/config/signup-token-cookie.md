# Signup Token Cookie

온보딩 전용 `signupToken`은 로그인 응답 body가 아니라 서버가 설정하는 HttpOnly 쿠키로 전달한다.

## 쿠키 정책

- 이름: `signup_token`
- 경로: `/api`
- 만료 시간: JWT의 `app.auth.jwt.signup-token-expiration`과 동일
- `HttpOnly`: 항상 활성화
- `SameSite`: `Lax`
- `Secure`: 개발 환경에서는 비활성화하고 운영 환경에서는 활성화
- CSRF 쿠키: `signup_csrf_token` (`HttpOnly`, 서버 검증용)
- CSRF 헤더: `X-Signup-CSRF-Token`

`SameSite=Lax` 정책과 CSRF 쿠키-헤더 일치 검증을 함께 적용한다. 프론트엔드와 API의 origin이 달라도 scheme과 site가 동일한 경우에만 쿠키가 전달된다. 예를 들어 `https://app.example.com`과 `https://api.example.com`은 동일 site이므로 연동할 수 있지만, 서로 다른 site에 배포하는 구성은 지원하지 않는다. 서버는 signup token 발급 응답의 `X-Signup-CSRF-Token` 헤더로 CSRF 값을 전달한다.

## 프론트엔드 연동

로그인 응답의 CSRF 헤더를 읽고, 온보딩 API 요청에 credential 옵션과 CSRF 헤더를 포함한다.

```ts
const loginResponse = await fetch(`${apiBaseUrl}/api/auth/login/kakao`, {
  method: "POST",
  credentials: "include",
  headers: {
    "Content-Type": "application/json",
  },
  body: JSON.stringify({ authorizationCode, redirectUri }),
});
const csrfToken = loginResponse.headers.get("X-Signup-CSRF-Token");
if (!csrfToken) throw new Error("Missing signup CSRF token");

fetch(`${apiBaseUrl}/api/onboarding/complete`, {
  method: "POST",
  credentials: "include",
  headers: {
    "X-Signup-CSRF-Token": csrfToken,
  },
});
```

카카오 로그인 요청 본문에는 `authorizationCode`와 `redirectUri`를 전달한다. 네이버 로그인도 동일한 필드 구조와 `POST` 메서드를 사용한다. 온보딩 프로필 수정 요청은 `PATCH`, 온보딩 완료 요청은 `POST` 메서드를 사용한다.

CSRF 헤더는 signup token으로 인증하는 상태 변경 요청에 포함한다. 서버는 CORS `exposedHeaders`에 해당 헤더를 등록하므로 허용된 origin의 프론트엔드에서 읽을 수 있다. 프론트엔드는 CSRF 값을 Web Storage가 아닌 메모리에 유지한다.

온보딩이 필요한 로그인 응답에는 `loginType: "SIGNUP_REQUIRED"`만 포함되며 `signupToken` 필드는 반환하지 않는다. 프론트엔드는 토큰을 Web Storage에 저장하거나 `Authorization` 헤더로 전달하지 않는다.

온보딩 완료 또는 유효하지 않은 signup token 감지 시 서버가 쿠키를 만료시켜 제거한다.
