package com.brifo.server.externalapi.dataserver

import java.math.BigDecimal
import java.time.LocalDate

data class DataServerResponse<T>(
    val success: Boolean,
    val code: String,
    val message: String,
    val result: T?,
)

data class DataServerStock(val name: String, val sector: String, val code: String)
data class DataServerPriceResult(val stock: DataServerStock, val stockPrice: DataServerStockPrice)
data class DataServerStockPrice(val price: BigDecimal, val changeRate: BigDecimal, val tradeDate: LocalDate)
