package com.brifo.server.badge.exception

import com.brifo.server.badge.code.BadgeErrorCode

class BadgeNotFoundException(
    message: String = BadgeErrorCode.BADGE_NOT_FOUND.message,
) : BadgeException(
    errorCode = BadgeErrorCode.BADGE_NOT_FOUND,
    message = message,
)
