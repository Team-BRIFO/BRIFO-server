package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode

class UnauthorizedException(
    message: String = AuthErrorCode.UNAUTHORIZED.message,
) : AuthException(AuthErrorCode.UNAUTHORIZED, message)
