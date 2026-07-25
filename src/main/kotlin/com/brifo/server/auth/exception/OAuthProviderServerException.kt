package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode

class OAuthProviderServerException(
    message: String = AuthErrorCode.OAUTH_PROVIDER_SERVER_ERROR.message,
) : AuthException(AuthErrorCode.OAUTH_PROVIDER_SERVER_ERROR, message)
