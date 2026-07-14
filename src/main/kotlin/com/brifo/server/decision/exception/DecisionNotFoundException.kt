package com.brifo.server.decision.exception

import com.brifo.server.decision.code.DecisionErrorCode

class DecisionNotFoundException(
    message: String = DecisionErrorCode.DECISION_NOT_FOUND.message,
) : DecisionException(
    errorCode = DecisionErrorCode.DECISION_NOT_FOUND,
    message = message,
)
