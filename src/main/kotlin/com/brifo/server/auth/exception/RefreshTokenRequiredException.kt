package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode

class RefreshTokenRequiredException(
    message: String = AuthErrorCode.REFRESH_TOKEN_REQUIRED.message,
) : AuthException(AuthErrorCode.REFRESH_TOKEN_REQUIRED, message)
