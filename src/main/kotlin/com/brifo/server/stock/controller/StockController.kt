package com.brifo.server.stock.controller

import com.brifo.server.global.common.ApiResponse
import com.brifo.server.global.code.SuccessCode
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

@RestController
@RequestMapping("/api")
class StockController(
    private val stockService: StockService,
) {
    @GetMapping("/stocks")
    fun getStocks(
        @RequestParam userId: Long,
        @Valid @ModelAttribute request: GetStocksRequest,
    ): ApiResponse<GetStocksResponse> =
        ApiResponse.success(
            code = SuccessCode.OK,
            result = stockService.getStocks(request),
        )

    @PatchMapping("/onboarding/interests")
    fun updateOnboardingInterests(
        @Valid @RequestBody request: UpdateStockInterestsRequest,
    ): ApiResponse<Nothing> = TODO("온보딩 관심 종목 저장 서비스 구현 필요")
}
