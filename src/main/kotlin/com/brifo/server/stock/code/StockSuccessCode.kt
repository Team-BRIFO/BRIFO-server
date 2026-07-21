package com.brifo.server.stock.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class StockSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    STOCK_INTERESTS_SAVED(
        HttpStatus.OK,
        "COMMON_200",
        "관심 종목이 저장되었습니다.",
    ),
}
