package com.brifo.server.stock.exception

import com.brifo.server.stock.code.StockErrorCode

class StockSelectionMinimumNotMetException(
    message: String = StockErrorCode.STOCK_SELECTION_MINIMUM_NOT_MET.message,
) : StockException(
    errorCode = StockErrorCode.STOCK_SELECTION_MINIMUM_NOT_MET,
    message = message,
)
