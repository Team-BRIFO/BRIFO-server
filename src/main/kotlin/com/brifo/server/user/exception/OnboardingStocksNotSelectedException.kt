package com.brifo.server.user.exception

import com.brifo.server.user.code.OnboardingErrorCode

class OnboardingStocksNotSelectedException(
    message: String = OnboardingErrorCode.STOCKS_NOT_SELECTED.message,
) : OnboardingException(
    errorCode = OnboardingErrorCode.STOCKS_NOT_SELECTED,
    message = message,
)
