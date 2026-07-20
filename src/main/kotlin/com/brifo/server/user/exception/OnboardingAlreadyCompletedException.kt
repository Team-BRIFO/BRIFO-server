package com.brifo.server.user.exception

import com.brifo.server.user.code.OnboardingErrorCode

class OnboardingAlreadyCompletedException(
    message: String = OnboardingErrorCode.ONBOARDING_ALREADY_COMPLETED.message,
) : OnboardingException(
    errorCode = OnboardingErrorCode.ONBOARDING_ALREADY_COMPLETED,
    message = message,
)
