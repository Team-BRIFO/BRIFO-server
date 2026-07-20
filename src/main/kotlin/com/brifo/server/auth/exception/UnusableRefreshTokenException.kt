package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode

class UnusableRefreshTokenException(
    message: String = AuthErrorCode.REFRESH_TOKEN_UNUSABLE.message,
) : AuthException(AuthErrorCode.REFRESH_TOKEN_UNUSABLE, message)
