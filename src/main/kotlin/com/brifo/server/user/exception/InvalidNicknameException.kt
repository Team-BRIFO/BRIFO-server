package com.brifo.server.user.exception

import com.brifo.server.user.code.UserErrorCode

class InvalidNicknameException(
    message: String = UserErrorCode.INVALID_NICKNAME.message,
) : UserException(
    errorCode = UserErrorCode.INVALID_NICKNAME,
    message = message,
)
