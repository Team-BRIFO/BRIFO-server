package com.brifo.server.policy.exception

import com.brifo.server.policy.code.PolicyErrorCode

class RequiredPolicyMissingException(
    message: String = PolicyErrorCode.REQUIRED_POLICY_MISSING.message,
) : PolicyException(
    errorCode = PolicyErrorCode.REQUIRED_POLICY_MISSING,
    message = message,
)
