package com.brifo.server.user.exception

import com.brifo.server.user.code.UserErrorCode

class PendingStockChangeNotFoundException(
    message: String = UserErrorCode.PENDING_STOCK_CHANGE_NOT_FOUND.message,
) : UserException(
    errorCode = UserErrorCode.PENDING_STOCK_CHANGE_NOT_FOUND,
    message = message,
)
