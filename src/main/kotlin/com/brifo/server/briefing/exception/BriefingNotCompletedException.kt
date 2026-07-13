package com.brifo.server.briefing.exception

import com.brifo.server.briefing.code.BriefingErrorCode

class BriefingNotCompletedException(
    message: String = BriefingErrorCode.BRIEFING_NOT_COMPLETED.message,
) : BriefingException(
    errorCode = BriefingErrorCode.BRIEFING_NOT_COMPLETED,
    message = message,
)
