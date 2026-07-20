package com.brifo.server.user.exception

import com.brifo.server.user.code.OnboardingErrorCode

class OnboardingProfileNotCompletedException(
    message: String = OnboardingErrorCode.PROFILE_NOT_COMPLETED.message,
) : OnboardingException(
    errorCode = OnboardingErrorCode.PROFILE_NOT_COMPLETED,
    message = message,
)
