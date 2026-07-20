package com.brifo.server.stock.dto.response

import com.brifo.server.global.common.CursorPage
import java.math.BigDecimal
import java.util.UUID

data class GetStocksResponse(
    val mode: Mode,
    val keyword: String?,
    val page: CursorPage<Item>,
) {
    data class Item(
        val rank: Int?,
        val stockId: UUID,
        val code: String,
        val name: String,
        val price: BigDecimal?,
        val changeRate: BigDecimal?,
    )

    enum class Mode {
        POPULAR,
        SEARCH,
    }
}
