package com.brifo.server.stock.dto.response

import com.brifo.server.global.common.CursorPage
import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.util.UUID

data class GetStocksResponse(
    val mode: Mode,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val keyword: String?,
    val page: CursorPage<Item>,
) {
    data class Item(
        @field:JsonInclude(JsonInclude.Include.NON_NULL)
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
