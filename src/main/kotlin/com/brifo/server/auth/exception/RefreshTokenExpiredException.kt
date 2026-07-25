package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode

class RefreshTokenExpiredException(
    message: String = AuthErrorCode.REFRESH_TOKEN_EXPIRED.message,
) : AuthException(AuthErrorCode.REFRESH_TOKEN_EXPIRED, message)
