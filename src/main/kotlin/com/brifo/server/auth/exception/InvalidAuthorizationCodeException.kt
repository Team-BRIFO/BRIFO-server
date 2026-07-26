package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode

class InvalidAuthorizationCodeException(
    message: String = AuthErrorCode.OAUTH_INVALID_AUTHORIZATION_CODE.message,
) : AuthException(AuthErrorCode.OAUTH_INVALID_AUTHORIZATION_CODE, message)
