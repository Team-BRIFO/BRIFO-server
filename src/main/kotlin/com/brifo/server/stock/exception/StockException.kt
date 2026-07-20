package com.brifo.server.stock.exception

import com.brifo.server.stock.code.StockErrorCode

import com.brifo.server.global.exception.BusinessException

sealed class StockException(
    errorCode: StockErrorCode,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)
