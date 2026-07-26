package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode

class RefreshTokenMismatchException(
    message: String = AuthErrorCode.REFRESH_TOKEN_MISMATCH.message,
) : AuthException(AuthErrorCode.REFRESH_TOKEN_MISMATCH, message)
