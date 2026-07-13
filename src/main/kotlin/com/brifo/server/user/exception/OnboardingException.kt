package com.brifo.server.user.exception

import com.brifo.server.user.code.OnboardingErrorCode

import com.brifo.server.global.exception.BusinessException

sealed class OnboardingException(
    errorCode: OnboardingErrorCode,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)
