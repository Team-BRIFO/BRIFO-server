package com.brifo.server.decision.exception

import com.brifo.server.decision.code.DecisionErrorCode

import com.brifo.server.global.exception.BusinessException

sealed class DecisionException(
    errorCode: DecisionErrorCode,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)
