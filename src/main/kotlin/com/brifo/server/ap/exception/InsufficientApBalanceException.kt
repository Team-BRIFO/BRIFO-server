package com.brifo.server.ap.exception

import com.brifo.server.ap.code.ApErrorCode

class InsufficientApBalanceException(
    message: String = ApErrorCode.INSUFFICIENT_AP_BALANCE.message,
) : ApException(
    errorCode = ApErrorCode.INSUFFICIENT_AP_BALANCE,
    message = message,
)
