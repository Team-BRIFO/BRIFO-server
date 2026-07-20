package com.brifo.server.briefing.exception

import com.brifo.server.briefing.code.BriefingErrorCode

class BriefingAgentNotInInitialRequestException(
    message: String = BriefingErrorCode.BRIEFING_AGENT_NOT_IN_INITIAL_REQUEST.message,
) : BriefingException(
    errorCode = BriefingErrorCode.BRIEFING_AGENT_NOT_IN_INITIAL_REQUEST,
    message = message,
)
