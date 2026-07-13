package com.brifo.server.briefing.exception

import com.brifo.server.briefing.code.BriefingErrorCode

class BriefingLimitExceededException(
    message: String = BriefingErrorCode.BRIEFING_LIMIT_EXCEEDED.message,
) : BriefingException(
    errorCode = BriefingErrorCode.BRIEFING_LIMIT_EXCEEDED,
    message = message,
)
