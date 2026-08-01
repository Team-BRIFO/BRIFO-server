package com.brifo.server.briefing.controller

import com.brifo.server.authenticatedUserId
import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.dto.response.BriefingStockResponse
import com.brifo.server.briefing.dto.response.CreateBriefingResponse
import com.brifo.server.briefing.dto.response.GetStockBriefingsResponse
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.briefing.entity.BriefingStatus
import com.brifo.server.briefing.exception.BriefingRetryCooldownException
import com.brifo.server.briefing.service.BriefingQueryService
import com.brifo.server.briefing.service.sync.BriefingRequestOrchestrator
import com.brifo.server.global.error.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class BriefingControllerTest {
    private val queryService = mock(BriefingQueryService::class.java)
    private val requestOrchestrator = mock(BriefingRequestOrchestrator::class.java)
    private val userId = authenticatedUserId()
    private val mockMvc: MockMvc = run {
        val validator = LocalValidatorFactoryBean().also { it.afterPropertiesSet() }
        MockMvcBuilders
            .standaloneSetup(BriefingController(queryService, requestOrchestrator))
            .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(GlobalExceptionHandler(MockEnvironment()))
            .setValidator(validator)
            .build()
    }

    @Test
    fun `브리핑 요청 성공은 201과 접수 결과를 반환한다`() {
        val stockId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        val briefingId = UUID.randomUUID()
        `when`(requestOrchestrator.request(userId, stockId, listOf(agentId))).thenReturn(
            CreateBriefingResponse(
                requestedCount = 1,
                totalSalaryCost = 10,
                requestedAgents = listOf(
                    CreateBriefingResponse.RequestedAgent(briefingId, agentId, AgentType.ROOKIE, 10),
                ),
            ),
        )

        mockMvc.perform(
            post("/api/stocks/{stockId}/briefings", stockId)
                .param("userId", userId.toString())
                .contentType("application/json")
                .content("""{"agentIds":["$agentId"]}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("COMMON_201"))
            .andExpect(jsonPath("$.result.requestedCount").value(1))
            .andExpect(jsonPath("$.result.totalSalaryCost").value(10))
            .andExpect(jsonPath("$.result.requestedAgents[0].briefingId").value(briefingId.toString()))
    }

    @Test
    fun `사원 목록이 비어 있으면 서비스 호출 전에 400을 반환한다`() {
        mockMvc.perform(
            post("/api/stocks/{stockId}/briefings", UUID.randomUUID())
                .param("userId", userId.toString())
                .contentType("application/json")
                .content("""{"agentIds":[]}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))
    }

    @Test
    fun `재의뢰 쿨다운은 429와 Retry-After를 반환한다`() {
        val stockId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        `when`(requestOrchestrator.request(userId, stockId, listOf(agentId)))
            .thenThrow(BriefingRetryCooldownException(17))

        mockMvc.perform(
            post("/api/stocks/{stockId}/briefings", stockId)
                .param("userId", userId.toString())
                .contentType("application/json")
                .content("""{"agentIds":["$agentId"]}"""),
        )
            .andExpect(status().isTooManyRequests)
            .andExpect(header().string("Retry-After", "17"))
            .andExpect(jsonPath("$.code").value("BRIEFING_429_01"))
    }

    @Test
    fun `종목 브리핑 조회는 모든 처리 상태와 미완료 null 결과를 직렬화한다`() {
        val stockId = UUID.randomUUID()
        val items = BriefingStatus.entries.map { statusValue ->
            GetStockBriefingsResponse.StockBriefingItem(
                briefingId = UUID.randomUUID(),
                status = statusValue,
                oneLiner = if (statusValue == BriefingStatus.COMPLETED) "완료 의견" else null,
                direction = if (statusValue == BriefingStatus.COMPLETED) BriefingDirection.UP else null,
                agentId = UUID.randomUUID(),
                nickname = statusValue.name,
                agentType = AgentType.ROOKIE,
            )
        }
        `when`(queryService.getStockBriefings(userId, stockId)).thenReturn(
            GetStockBriefingsResponse(
                stock = BriefingStockResponse(
                    stockId,
                    "삼성전자",
                    BigDecimal("72420"),
                    BigDecimal("2.0"),
                    LocalDate.of(2026, 7, 18),
                ),
                items = items,
            ),
        )

        mockMvc.perform(
            get("/api/stocks/{stockId}/briefing", stockId)
                .param("userId", userId.toString()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.items.length()").value(4))
            .andExpect(jsonPath("$.result.items[0].status").value("PENDING"))
            .andExpect(jsonPath("$.result.items[0].oneLiner").isEmpty)
            .andExpect(jsonPath("$.result.items[1].status").value("ANALYZING"))
            .andExpect(jsonPath("$.result.items[2].status").value("COMPLETED"))
            .andExpect(jsonPath("$.result.items[2].direction").value("UP"))
            .andExpect(jsonPath("$.result.items[3].status").value("FAILED"))
    }
}
