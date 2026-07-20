package com.brifo.server.briefing.exception

import com.brifo.server.briefing.code.BriefingErrorCode

class BriefingRetryCooldownException(
    val retryAfterSeconds: Long,
) : BriefingException(
    errorCode = BriefingErrorCode.BRIEFING_RETRY_COOLDOWN,
)
