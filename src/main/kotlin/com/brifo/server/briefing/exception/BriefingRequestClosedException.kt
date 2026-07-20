package com.brifo.server.briefing.exception

import com.brifo.server.briefing.code.BriefingErrorCode

class BriefingRequestClosedException(
    message: String = BriefingErrorCode.BRIEFING_REQUEST_CLOSED.message,
) : BriefingException(
    errorCode = BriefingErrorCode.BRIEFING_REQUEST_CLOSED,
    message = message,
)
