package com.brifo.server.auth.exception

import com.brifo.server.auth.code.AuthErrorCode

class KakaoServerException(
    message: String = AuthErrorCode.KAKAO_SERVER_ERROR.message,
) : AuthException(AuthErrorCode.KAKAO_SERVER_ERROR, message)
