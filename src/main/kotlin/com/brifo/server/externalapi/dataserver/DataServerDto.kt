package com.brifo.server.externalapi.dataserver

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DataServerResponse<T>(
    val success: Boolean,
    val code: String,
    val message: String,
    val result: T?,
)

data class DataServerStock(val name: String, val sector: String, val code: String)
data class DataServerNewsResult(val stock: DataServerStock, val news: List<DataServerNews> = emptyList())
data class DataServerNews(
    val newsId: UUID,
    val title: String,
    val content: String,
    val sourceUrl: String,
    val imageUrl: String?,
    val publishedAt: LocalDateTime,
)
data class DataServerPriceResult(val stock: DataServerStock, val stockPrice: DataServerStockPrice)
data class DataServerStockPrice(val price: BigDecimal, val changeRate: BigDecimal, val tradeDate: LocalDate)
data class DataServerDisclosureResult(val stock: DataServerStock, val disclosure: DataServerDisclosure)
data class DataServerDisclosure(val hasDisclosure: Boolean)
