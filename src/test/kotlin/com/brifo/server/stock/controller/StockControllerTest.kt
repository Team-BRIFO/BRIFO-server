package com.brifo.server.stock.controller

import com.brifo.server.global.common.CursorPage
import com.brifo.server.stock.dto.request.GetStocksRequest
import com.brifo.server.stock.dto.response.GetStocksResponse
import com.brifo.server.stock.service.StockService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.util.UUID

@WebMvcTest(StockController::class)
@AutoConfigureMockMvc(addFilters = false)
class StockControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var stockService: StockService

    @Test
    fun `size 기본값과 1부터 50까지의 경계값을 허용한다`() {
        listOf(20, 1, 50).forEach { size ->
            val request = GetStocksRequest(keyword = "삼성", size = size)
            `when`(stockService.getStocks(request)).thenReturn(searchResponse())

            val requestBuilder =
                get("/api/stocks")
                    .param("userId", "7")
                    .param("keyword", "삼성")

            // 기본값 20은 size 파라미터를 전달하지 않고 검증한다.
            if (size != 20) {
                requestBuilder.param("size", size.toString())
            }

            mockMvc
                .perform(requestBuilder)
                .andExpect(status().isOk)
        }
    }

    @Test
    fun `인기 모드에서는 범위를 벗어난 size도 무시한다`() {
        listOf("0", "51").forEach { size ->
            val request = GetStocksRequest(size = size.toInt())
            `when`(stockService.getStocks(request)).thenReturn(popularResponse())

            mockMvc
                .perform(
                    get("/api/stocks")
                        .param("userId", "7")
                        .param("size", size),
                ).andExpect(status().isOk)
        }
    }

    private fun popularResponse(): GetStocksResponse =
        GetStocksResponse(
            mode = GetStocksResponse.Mode.POPULAR,
            keyword = null,
            page =
                CursorPage(
                    items = emptyList(),
                    nextCursor = null,
                    hasNext = false,
                ),
        )

    private fun searchResponse(): GetStocksResponse =
        GetStocksResponse(
            mode = GetStocksResponse.Mode.SEARCH,
            keyword = "삼성",
            page =
                CursorPage(
                    items =
                        listOf(
                            GetStocksResponse.StockItem(
                                rank = null,
                                stockId = UUID.randomUUID(),
                                code = "005930",
                                name = "삼성전자",
                                logoUrl = "https://cdn.example.com/005930.png",
                                price = BigDecimal("71000.00"),
                                changeRate = BigDecimal("2.1"),
                            ),
                        ),
                    nextCursor = null,
                    hasNext = false,
                ),
        )
}
