package com.brifo.server.decision.exception

import com.brifo.server.decision.code.DecisionErrorCode

class DecisionNotSettledException(
    message: String = DecisionErrorCode.DECISION_NOT_SETTLED.message,
) : DecisionException(
    errorCode = DecisionErrorCode.DECISION_NOT_SETTLED,
    message = message,
)
