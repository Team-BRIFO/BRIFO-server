package com.brifo.server.ap.exception

import com.brifo.server.ap.code.ApErrorCode

class CreditLoanNotEligibleException(
    message: String = ApErrorCode.CREDIT_LOAN_NOT_ELIGIBLE.message,
) : ApException(
    errorCode = ApErrorCode.CREDIT_LOAN_NOT_ELIGIBLE,
    message = message,
)
