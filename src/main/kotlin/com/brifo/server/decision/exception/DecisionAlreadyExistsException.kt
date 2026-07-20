package com.brifo.server.decision.exception

import com.brifo.server.decision.code.DecisionErrorCode

class DecisionAlreadyExistsException(
    message: String = DecisionErrorCode.DECISION_ALREADY_EXISTS.message,
) : DecisionException(
    errorCode = DecisionErrorCode.DECISION_ALREADY_EXISTS,
    message = message,
)
