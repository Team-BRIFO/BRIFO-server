package com.brifo.server.stock.exception

import com.brifo.server.stock.code.StockErrorCode

class StockNotFoundException(
    message: String = StockErrorCode.STOCK_NOT_FOUND.message,
) : StockException(
    errorCode = StockErrorCode.STOCK_NOT_FOUND,
    message = message,
)
