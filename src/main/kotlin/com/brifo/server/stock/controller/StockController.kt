package com.brifo.server.stock.controller

import com.brifo.server.global.common.ApiResponse
import com.brifo.server.global.code.SuccessCode
import com.brifo.server.stock.dto.request.GetStocksRequest
import com.brifo.server.stock.dto.response.GetStocksResponse
import com.brifo.server.stock.service.StockService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class StockController(
    private val stockService: StockService,
) {
    @GetMapping("/stocks")
    fun getStocks(
        @Valid @ModelAttribute request: GetStocksRequest,
    ): ApiResponse<GetStocksResponse> =
        ApiResponse.success(
            code = SuccessCode.OK,
            result = stockService.getStocks(request),
        )
}
