package com.brifo.server.user.exception

import com.brifo.server.user.code.UserErrorCode

class InvalidCompanyNameException(
    message: String = UserErrorCode.INVALID_COMPANY_NAME.message,
) : UserException(
    errorCode = UserErrorCode.INVALID_COMPANY_NAME,
    message = message,
)
