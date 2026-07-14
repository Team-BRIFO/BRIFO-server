package com.brifo.server.policy.exception

import com.brifo.server.policy.code.PolicyErrorCode

class PolicyNotFoundException(
    message: String = PolicyErrorCode.POLICY_NOT_FOUND.message,
) : PolicyException(
    errorCode = PolicyErrorCode.POLICY_NOT_FOUND,
    message = message,
)
