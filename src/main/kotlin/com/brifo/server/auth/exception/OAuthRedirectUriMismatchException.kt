package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode

class OAuthRedirectUriMismatchException(
    message: String = AuthErrorCode.OAUTH_REDIRECT_URI_MISMATCH.message,
) : AuthException(AuthErrorCode.OAUTH_REDIRECT_URI_MISMATCH, message)
