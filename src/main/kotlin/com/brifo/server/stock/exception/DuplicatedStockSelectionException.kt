package com.brifo.server.stock.exception

import com.brifo.server.stock.code.StockErrorCode

class DuplicatedStockSelectionException(
    message: String = StockErrorCode.STOCK_SELECTION_DUPLICATED.message,
) : StockException(
    errorCode = StockErrorCode.STOCK_SELECTION_DUPLICATED,
    message = message,
)
