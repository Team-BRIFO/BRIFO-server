package com.brifo.server.briefing.exception

import com.brifo.server.briefing.code.BriefingErrorCode

import com.brifo.server.global.exception.BusinessException

sealed class BriefingException(
    errorCode: BriefingErrorCode,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)
