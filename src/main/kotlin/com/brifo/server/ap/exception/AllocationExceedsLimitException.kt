package com.brifo.server.ap.exception

import com.brifo.server.ap.code.ApErrorCode

class AllocationExceedsLimitException(
    message: String = ApErrorCode.ALLOCATION_EXCEEDS_LIMIT.message,
) : ApException(
    errorCode = ApErrorCode.ALLOCATION_EXCEEDS_LIMIT,
    message = message,
)
