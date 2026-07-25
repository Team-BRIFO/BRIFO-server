package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode
import com.brifo.server.global.exception.BusinessException

sealed class AuthException(
    errorCode: AuthErrorCode,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)
