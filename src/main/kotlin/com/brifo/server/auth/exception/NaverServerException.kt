package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode

class NaverServerException(
    message: String = AuthErrorCode.NAVER_SERVER_ERROR.message,
) : AuthException(AuthErrorCode.NAVER_SERVER_ERROR, message)
