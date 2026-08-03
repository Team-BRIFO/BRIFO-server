# Signup Token Cookie

온보딩 전용 `signupToken`은 로그인 응답 body가 아니라 서버가 설정하는 HttpOnly 쿠키로 전달한다.

## 쿠키 정책

- 이름: `signup_token`
- 경로: `/api`
- 만료 시간: JWT의 `app.auth.jwt.signup-token-expiration`과 동일
- `HttpOnly`: 항상 활성화
- `SameSite`: `Lax`
- `Secure`: 개발 환경에서는 비활성화하고 운영 환경에서는 활성화

`SameSite=Lax` 정책을 사용하므로 프론트엔드와 API는 동일 사이트에 배포해야 한다. 서로 다른 사이트에서 쿠키를 전달해야 한다면 `SameSite=None; Secure`로 바꾸기 전에 별도의 CSRF 방어를 먼저 도입한다.

## 프론트엔드 연동

로그인과 온보딩 API 요청에 credential 옵션을 포함한다.

```ts
fetch(url, {
  credentials: "include",
});
```

온보딩이 필요한 로그인 응답에는 `loginType: "SIGNUP_REQUIRED"`만 포함되며 `signupToken` 필드는 반환하지 않는다. 프론트엔드는 토큰을 Web Storage에 저장하거나 `Authorization` 헤더로 전달하지 않는다.

온보딩 완료 또는 유효하지 않은 signup token 감지 시 서버가 쿠키를 만료시켜 제거한다.
