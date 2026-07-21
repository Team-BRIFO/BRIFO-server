package com.brifo.server.stock.controller

import com.brifo.server.global.error.GlobalExceptionHandler
import com.brifo.server.stock.service.StockService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.UUID

class StockControllerTest {
    private val stockService = mock(StockService::class.java)

    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(StockController(stockService))
            .setControllerAdvice(GlobalExceptionHandler(MockEnvironment()))
            .build()

    @Test
    fun `관심 종목 저장 성공은 200을 반환한다`() {
        val userId = UUID.randomUUID()
        val stockId = UUID.randomUUID()

        mockMvc
            .perform(
                patch("/api/stocks")
                    .param("userId", userId.toString())
                    .contentType("application/json")
                    .content(
                        """
                        {
                          "stockIds": ["$stockId"]
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("COMMON_200"))
            .andExpect(jsonPath("$.message").value("관심 종목이 저장되었습니다."))
            .andExpect(jsonPath("$.result").doesNotExist())

        verify(stockService).updateInterests(
            userPublicId = userId,
            stockIds = listOf(stockId),
        )
    }

    @Test
    fun `userId가 없으면 400을 반환한다`() {
        mockMvc
            .perform(
                patch("/api/stocks")
                    .contentType("application/json")
                    .content(
                        """
                        {
                          "stockIds": ["${UUID.randomUUID()}"]
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)
    }
}
