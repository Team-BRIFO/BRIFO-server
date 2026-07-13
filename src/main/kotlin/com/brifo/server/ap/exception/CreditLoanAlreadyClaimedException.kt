package com.brifo.server.ap.exception

import com.brifo.server.ap.code.ApErrorCode

class CreditLoanAlreadyClaimedException(
    message: String = ApErrorCode.CREDIT_LOAN_ALREADY_CLAIMED.message,
) : ApException(
    errorCode = ApErrorCode.CREDIT_LOAN_ALREADY_CLAIMED,
    message = message,
)
