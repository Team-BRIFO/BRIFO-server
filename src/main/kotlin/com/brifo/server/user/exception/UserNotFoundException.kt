package com.brifo.server.user.exception

import com.brifo.server.user.code.UserErrorCode

class UserNotFoundException(
    message: String = UserErrorCode.USER_NOT_FOUND.message,
) : UserException(
    errorCode = UserErrorCode.USER_NOT_FOUND,
    message = message,
)
