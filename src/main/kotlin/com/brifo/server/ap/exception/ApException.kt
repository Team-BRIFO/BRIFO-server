package com.brifo.server.ap.exception

import com.brifo.server.ap.code.ApErrorCode

import com.brifo.server.global.exception.BusinessException

sealed class ApException(
    errorCode: ApErrorCode,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)
