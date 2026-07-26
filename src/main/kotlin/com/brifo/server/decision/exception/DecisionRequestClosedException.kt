package com.brifo.server.decision.exception

import com.brifo.server.decision.code.DecisionErrorCode

class DecisionRequestClosedException(
    message: String = DecisionErrorCode.DECISION_REQUEST_CLOSED.message,
) : DecisionException(
    errorCode = DecisionErrorCode.DECISION_REQUEST_CLOSED,
    message = message,
)
