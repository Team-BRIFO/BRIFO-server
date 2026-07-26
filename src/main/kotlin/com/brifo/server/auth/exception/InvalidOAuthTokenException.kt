package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode

class InvalidOAuthTokenException(
    message: String = AuthErrorCode.INVALID_TOKEN.message,
) : AuthException(AuthErrorCode.INVALID_TOKEN, message)
