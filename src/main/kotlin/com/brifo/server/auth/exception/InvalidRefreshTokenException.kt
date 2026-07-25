package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode

class InvalidRefreshTokenException(
    message: String = AuthErrorCode.REFRESH_TOKEN_INVALID.message,
) : AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID, message)
