package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode

class ForbiddenException(
    message: String = AuthErrorCode.FORBIDDEN.message,
) : AuthException(AuthErrorCode.FORBIDDEN, message)
