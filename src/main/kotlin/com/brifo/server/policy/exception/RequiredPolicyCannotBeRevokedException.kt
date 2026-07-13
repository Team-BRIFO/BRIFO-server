package com.brifo.server.policy.exception

import com.brifo.server.policy.code.PolicyErrorCode

class RequiredPolicyCannotBeRevokedException(
    message: String = PolicyErrorCode.REQUIRED_POLICY_CANNOT_BE_REVOKED.message,
) : PolicyException(
    errorCode = PolicyErrorCode.REQUIRED_POLICY_CANNOT_BE_REVOKED,
    message = message,
)
