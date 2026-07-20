package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode

class AuthUserNotFoundException(
    message: String = AuthErrorCode.AUTH_USER_NOT_FOUND.message,
) : AuthException(AuthErrorCode.AUTH_USER_NOT_FOUND, message)
