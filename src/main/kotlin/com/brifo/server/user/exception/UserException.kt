package com.brifo.server.user.exception

import com.brifo.server.user.code.UserErrorCode

import com.brifo.server.global.exception.BusinessException

sealed class UserException(
    errorCode: UserErrorCode,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)
