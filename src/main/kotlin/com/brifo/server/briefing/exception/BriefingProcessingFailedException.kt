package com.brifo.server.briefing.exception

import com.brifo.server.briefing.code.BriefingErrorCode

class BriefingProcessingFailedException(
    message: String = BriefingErrorCode.BRIEFING_PROCESSING_FAILED.message,
) : BriefingException(
    errorCode = BriefingErrorCode.BRIEFING_PROCESSING_FAILED,
    message = message,
)
