package com.brifo.server.policy.exception

import com.brifo.server.policy.code.PolicyErrorCode

import com.brifo.server.global.exception.BusinessException

sealed class PolicyException(
    errorCode: PolicyErrorCode,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)
