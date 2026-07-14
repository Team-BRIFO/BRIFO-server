package com.brifo.server.user.exception

import com.brifo.server.user.code.OnboardingErrorCode

class RequiredPoliciesNotAgreedException(
    message: String = OnboardingErrorCode.REQUIRED_POLICIES_NOT_AGREED.message,
) : OnboardingException(
    errorCode = OnboardingErrorCode.REQUIRED_POLICIES_NOT_AGREED,
    message = message,
)
