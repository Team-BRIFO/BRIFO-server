package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode

class InvalidTokenException(
    message: String = AuthErrorCode.OAUTH_INVALID_TOKEN.message,
) : AuthException(AuthErrorCode.OAUTH_INVALID_TOKEN, message)
