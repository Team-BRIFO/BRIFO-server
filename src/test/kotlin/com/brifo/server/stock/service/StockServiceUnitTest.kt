package com.brifo.server.stock.service

import com.brifo.server.global.exception.BusinessException
import com.brifo.server.stock.dto.request.GetStocksRequest
import com.brifo.server.stock.dto.response.GetStocksResponse
import com.brifo.server.stock.repository.StockRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class StockServiceUnitTest {
    private lateinit var stockRepository: StockRepository
    private lateinit var stockService: StockService

    @BeforeEach
    fun setUp() {
        stockRepository = mock(StockRepository::class.java)
        stockService = StockService(stockRepository)
    }

    @Test
    fun `검색어가 공백이면 인기 모드의 순위와 페이지 정보를 반환한다`() {
        val stocks =
            listOf(
                stockItem(
                    name = "삼성전자",
                    changeRate = BigDecimal("2.14"),
                ),
                stockItem(
                    name = "SK하이닉스",
                    changeRate = BigDecimal("-1.25"),
                ),
            )
        `when`(stockRepository.findPopularStocks()).thenReturn(stocks)

        // 인기 모드에서는 요청의 cursor와 size를 사용하지 않는다.
        val response =
            stockService.getStocks(
                GetStocksRequest(
                    keyword = "   ",
                    cursor = UUID.randomUUID(),
                    size = 0,
                ),
            )

        // 조회 순서대로 순위를 부여하고 등락률을 소수점 한 자리로 반올림한다.
        assertEquals(GetStocksResponse.Mode.POPULAR, response.mode)
        assertNull(response.keyword)
        assertEquals(listOf(1, 2), response.page.items.map { it.rank })
        assertEquals(BigDecimal("2.1"), response.page.items[0].changeRate)
        assertEquals(BigDecimal("-1.3"), response.page.items[1].changeRate)

        // 인기 모드는 페이지네이션을 사용하지 않는다.
        assertNull(response.page.nextCursor)
        assertFalse(response.page.hasNext)
    }

    @Test
    fun `검색 결과가 size보다 많으면 다음 커서를 반환한다`() {
        val cursor = UUID.randomUUID()
        val stocks =
            listOf(
                stockItem("삼성전자"),
                stockItem("삼성전자우"),
                stockItem("삼성전자 테스트"),
            )

        // 다음 페이지 여부를 판단하기 위해 size보다 한 개 더 조회한다.
        `when`(
            stockRepository.searchStocks(
                keyword = "삼성 전자",
                cursor = cursor,
                limit = 3,
            ),
        ).thenReturn(stocks)

        val response =
            stockService.getStocks(
                GetStocksRequest(
                    keyword = "  삼성 전자  ",
                    cursor = cursor,
                    size = 2,
                ),
            )

        // 실제 응답에는 요청한 size만큼만 포함한다.
        assertEquals(GetStocksResponse.Mode.SEARCH, response.mode)
        assertEquals("삼성 전자", response.keyword)
        assertEquals(2, response.page.items.size)
        assertTrue(response.page.items.all { it.rank == null })

        // 다음 데이터가 있으면 반환된 마지막 종목을 다음 커서로 사용한다.
        assertTrue(response.page.hasNext)
        assertEquals(stocks[1].stockId, response.page.nextCursor)
    }

    @Test
    fun `검색 결과가 없으면 빈 페이지를 반환한다`() {
        `when`(
            stockRepository.searchStocks(
                keyword = "없는종목",
                cursor = null,
                limit = 21,
            ),
        ).thenReturn(emptyList())

        val response =
            stockService.getStocks(
                GetStocksRequest(keyword = "없는종목"),
            )

        assertEquals(GetStocksResponse.Mode.SEARCH, response.mode)
        assertEquals("없는종목", response.keyword)
        assertTrue(response.page.items.isEmpty())
        assertFalse(response.page.hasNext)
        assertNull(response.page.nextCursor)
    }

    @Test
    fun `검색 모드에서 size가 허용 범위를 벗어나면 예외를 던진다`() {
        listOf(0, 51).forEach { size ->
            assertThrows<BusinessException> {
                stockService.getStocks(
                    GetStocksRequest(
                        keyword = "삼성",
                        size = size,
                    ),
                )
            }
        }
    }

    private fun stockItem(
        name: String,
        changeRate: BigDecimal? = BigDecimal("1.0"),
    ): GetStocksResponse.Item =
        GetStocksResponse.Item(
            rank = null,
            stockId = UUID.randomUUID(),
            code = "005930",
            name = name,
            price = BigDecimal("71000.00"),
            changeRate = changeRate,
        )
}
