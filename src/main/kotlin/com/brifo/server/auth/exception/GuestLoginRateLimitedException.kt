package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode

class GuestLoginRateLimitedException(
    val retryAfterSeconds: Long,
) : AuthException(AuthErrorCode.GUEST_LOGIN_RATE_LIMITED)
