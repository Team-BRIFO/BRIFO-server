package com.brifo.server.decision.controller

import com.brifo.server.authenticatedUserId
import com.brifo.server.agent.entity.AgentType
import com.brifo.server.decision.dto.response.CreateDecisionResponse
import com.brifo.server.decision.dto.response.GetDecisionResultResponse
import com.brifo.server.decision.dto.response.GetDecisionsResponse
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.decision.exception.DecisionNotSettledException
import com.brifo.server.decision.service.DecisionQueryService
import com.brifo.server.decision.service.DecisionRequestService
import com.brifo.server.global.error.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class DecisionControllerTest {
    private val requestService = mock(DecisionRequestService::class.java)
    private val queryService = mock(DecisionQueryService::class.java)
    private val userId = authenticatedUserId()
    private val mockMvc: MockMvc = run {
        val validator = LocalValidatorFactoryBean().also { it.afterPropertiesSet() }
        MockMvcBuilders
            .standaloneSetup(DecisionController(requestService, queryService))
            .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(GlobalExceptionHandler(MockEnvironment()))
            .setValidator(validator)
            .build()
    }

    @Test
    fun `결정 등록 성공은 201과 생성 결과를 반환한다`() {
        val briefingId = UUID.randomUUID()
        val decisionId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        `when`(
            requestService.request(
                userId,
                briefingId,
                DecisionDirection.UP,
                4,
            ),
        ).thenReturn(
            CreateDecisionResponse(
                decisionId = decisionId,
                direction = DecisionDirection.UP,
                confidenceLevel = 4,
                stock = CreateDecisionResponse.Stock(stockId, "삼성전자"),
            ),
        )

        mockMvc.perform(
            post("/api/briefings/{briefingId}/decisions", briefingId)
                .param("userId", userId.toString())
                .contentType("application/json")
                .content("""{"direction":"UP","confidenceLevel":4}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("COMMON_201"))
            .andExpect(jsonPath("$.result.decisionId").value(decisionId.toString()))
    }

    @Test
    fun `확신도 범위를 벗어나면 400을 반환한다`() {
        mockMvc.perform(
            post("/api/briefings/{briefingId}/decisions", UUID.randomUUID())
                .param("userId", userId.toString())
                .contentType("application/json")
                .content("""{"direction":"UP","confidenceLevel":6}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))
    }

    @Test
    fun `시세가 없어도 오늘의 미정산 결정은 null 시세와 함께 반환한다`() {
        val decisionId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        `when`(queryService.getDecisions(userId)).thenReturn(
            GetDecisionsResponse(
                items = listOf(
                    GetDecisionsResponse.Item(
                        decisionId = decisionId,
                        direction = DecisionDirection.DOWN,
                        confidenceLevel = 3,
                        agent = GetDecisionsResponse.Agent(agentId, AgentType.TANKER),
                        stock = GetDecisionsResponse.Stock(stockId, "삼성전자", null, null, null),
                    ),
                ),
            ),
        )

        mockMvc.perform(
            get("/api/decisions").param("userId", userId.toString()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.items[0].decisionId").value(decisionId.toString()))
            .andExpect(jsonPath("$.result.items[0].agent.agentId").value(agentId.toString()))
            .andExpect(jsonPath("$.result.items[0].agent.agentType").value("TANKER"))
            .andExpect(jsonPath("$.result.items[0].stock.stockId").value(stockId.toString()))
            .andExpect(jsonPath("$.result.items[0].stock.price").isEmpty)
            .andExpect(jsonPath("$.result.items[0].stock.changeRate").isEmpty)
            .andExpect(jsonPath("$.result.items[0].stock.tradeDate").isEmpty)
    }

    @Test
    fun `정산 결과는 판단 가격과 채택 사원을 반환한다`() {
        val decisionId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        `when`(queryService.getDecisionResult(userId, decisionId)).thenReturn(
            GetDecisionResultResponse(
                isCorrect = true,
                apDelta = 80,
                direction = DecisionDirection.UP,
                confidenceLevel = 4,
                agent = GetDecisionResultResponse.Agent(agentId, AgentType.ROOKIE),
                stock = GetDecisionResultResponse.Stock(
                    name = "삼성전자",
                    price = 72_420,
                    changeRate = BigDecimal("2.0"),
                    tradeDate = LocalDate.of(2026, 7, 21),
                ),
            ),
        )

        mockMvc.perform(
            get("/api/decisions/{decisionId}", decisionId)
                .param("userId", userId.toString()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.isCorrect").value(true))
            .andExpect(jsonPath("$.result.agent.agentId").value(agentId.toString()))
            .andExpect(jsonPath("$.result.stock.price").value(72420))
    }

    @Test
    fun `정산 전 결과 조회는 409를 반환한다`() {
        val decisionId = UUID.randomUUID()
        `when`(queryService.getDecisionResult(userId, decisionId)).thenThrow(DecisionNotSettledException())

        mockMvc.perform(
            get("/api/decisions/{decisionId}", decisionId)
                .param("userId", userId.toString()),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("DECISION_409_03"))
    }
}
