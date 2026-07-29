package com.brifo.server.stock.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class StockErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    STOCK_SELECTION_MINIMUM_NOT_MET(
        HttpStatus.BAD_REQUEST,
        "STOCK_400_01",
        "관심 종목은 최소 1개 이상 선택해야 합니다.",
    ),
    STOCK_SELECTION_MAXIMUM_EXCEEDED(
        HttpStatus.BAD_REQUEST,
        "STOCK_400_02",
        "관심 종목은 최대 3개까지 선택할 수 있습니다.",
    ),
    STOCK_SELECTION_DUPLICATED(
        HttpStatus.BAD_REQUEST,
        "STOCK_400_03",
        "중복된 관심 종목이 포함되어 있습니다.",
    ),
    STOCK_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "STOCK_404",
        "종목을 찾을 수 없습니다.",
    ),
    STOCK_PRICE_UNAVAILABLE(
        HttpStatus.SERVICE_UNAVAILABLE,
        "STOCK_503_01",
        "현재 주식 가격을 조회할 수 없습니다.",
    ),
}
