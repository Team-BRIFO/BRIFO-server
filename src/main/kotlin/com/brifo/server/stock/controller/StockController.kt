package com.brifo.server.stock.controller

import com.brifo.server.global.common.ApiResponse
import com.brifo.server.stock.code.StockSuccessCode
import com.brifo.server.stock.dto.request.GetStocksRequest
import com.brifo.server.stock.dto.request.UpdateStockInterestsRequest
import com.brifo.server.stock.dto.response.GetStocksResponse
import com.brifo.server.stock.service.StockService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// TODO: Spring Security 적용 후 userId 요청 파라미터를 인증 사용자 정보로 교체
@RestController
@RequestMapping("/api")
class StockController(
    private val stockService: StockService,
) {
    @GetMapping("/stocks")
    fun getStocks(
        @ModelAttribute request: GetStocksRequest,
    ): ApiResponse<GetStocksResponse> = TODO("종목 조회 서비스 구현 필요")

    @PatchMapping("/stocks")
    fun updateStockInterests(
        @RequestParam userId: UUID,
        @Valid @RequestBody request: UpdateStockInterestsRequest,
    ): ApiResponse<Nothing> {
        stockService.updateInterests(
            userPublicId = userId,
            stockIds = request.stockIds,
        )

        return ApiResponse.success(StockSuccessCode.STOCK_INTERESTS_SAVED)
    }
}
