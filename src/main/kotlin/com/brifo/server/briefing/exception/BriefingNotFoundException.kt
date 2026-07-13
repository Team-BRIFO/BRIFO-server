package com.brifo.server.briefing.exception

import com.brifo.server.briefing.code.BriefingErrorCode

class BriefingNotFoundException(
    message: String = BriefingErrorCode.BRIEFING_NOT_FOUND.message,
) : BriefingException(
    errorCode = BriefingErrorCode.BRIEFING_NOT_FOUND,
    message = message,
)
