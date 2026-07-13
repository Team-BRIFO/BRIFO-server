package com.brifo.server.stock.exception

import com.brifo.server.stock.code.StockErrorCode

class StockSelectionMaximumExceededException(
    message: String = StockErrorCode.STOCK_SELECTION_MAXIMUM_EXCEEDED.message,
) : StockException(
    errorCode = StockErrorCode.STOCK_SELECTION_MAXIMUM_EXCEEDED,
    message = message,
)
