package com.brifo.server.policy.exception

import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.exception.BusinessException

class DuplicatedPolicyIdsException(
    message: String = ErrorCode.INVALID_REQUEST.message,
) : BusinessException(
        errorCode = ErrorCode.INVALID_REQUEST,
        message = message,
    )
