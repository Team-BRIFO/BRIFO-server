package com.brifo.server.briefing.exception

import com.brifo.server.briefing.code.BriefingErrorCode

class BriefingAlreadyRequestedException(
    message: String = BriefingErrorCode.BRIEFING_ALREADY_REQUESTED.message,
) : BriefingException(
    errorCode = BriefingErrorCode.BRIEFING_ALREADY_REQUESTED,
    message = message,
)
