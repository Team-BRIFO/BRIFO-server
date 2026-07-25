package com.brifo.server.auth.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class AuthErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    REFRESH_TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "AUTH_400_02", "refreshToken은 필수입니다."),
    OAUTH_REDIRECT_URI_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_400_01", "Redirect URI가 일치하지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "인증이 필요합니다."),
    OAUTH_INVALID_AUTHORIZATION_CODE(HttpStatus.UNAUTHORIZED, "AUTH_401_01", "유효하지 않은 인증 코드입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_03", "유효하지 않은 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_401_04", "Refresh Token이 만료되었습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_401_05", "유효하지 않은 Refresh Token입니다."),
    REFRESH_TOKEN_UNUSABLE(HttpStatus.UNAUTHORIZED, "AUTH_401_06", "사용할 수 없는 Refresh Token입니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH_401_07", "Refresh Token이 일치하지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_403", "접근 권한이 없습니다."),
    AUTH_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_404_01", "사용자를 찾을 수 없습니다."),
    OAUTH_PROVIDER_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "AUTH_502", "OAuth 서버 오류가 발생했습니다."),
}
