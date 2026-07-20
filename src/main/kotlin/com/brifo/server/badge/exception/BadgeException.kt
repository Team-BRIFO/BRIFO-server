package com.brifo.server.badge.exception

import com.brifo.server.badge.code.BadgeErrorCode

import com.brifo.server.global.exception.BusinessException

sealed class BadgeException(
    errorCode: BadgeErrorCode,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)
