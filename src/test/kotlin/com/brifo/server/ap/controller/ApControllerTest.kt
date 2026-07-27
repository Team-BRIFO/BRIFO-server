package com.brifo.server.ap.controller

import com.brifo.server.authenticatedUserId
import com.brifo.server.ap.dto.request.CreateCreditLoanRequest
import com.brifo.server.ap.dto.request.GetApTransactionsRequest
import com.brifo.server.ap.dto.response.ApBalanceResponse
import com.brifo.server.ap.dto.response.CreateAttendanceRewardResponse
import com.brifo.server.ap.dto.response.GetApTransactionsResponse
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.exception.AttendanceRewardAlreadyClaimedException
import com.brifo.server.ap.service.ApService
import com.brifo.server.global.common.CursorPage
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
import java.time.LocalDateTime
import java.util.UUID

class ApControllerTest {
    private val service = mock(ApService::class.java)
    private val userId = authenticatedUserId()
    private val mockMvc: MockMvc = run {
        val validator = LocalValidatorFactoryBean().also { it.afterPropertiesSet() }
        MockMvcBuilders
            .standaloneSetup(ApController(service))
            .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(GlobalExceptionHandler(MockEnvironment()))
            .setValidator(validator)
            .build()
    }

    @Test
    fun `AP 거래 조회는 요약과 공개 거래 ID 커서를 반환한다`() {
        val transactionId = UUID.randomUUID()
        `when`(service.getApTransactions(userId, GetApTransactionsRequest(size = 1))).thenReturn(
            GetApTransactionsResponse(
                summary = GetApTransactionsResponse.Summary(120, 80, 40),
                page =
                    CursorPage(
                        items =
                            listOf(
                                GetApTransactionsResponse.Item(
                                    transactionId,
                                    ApTransactionReason.DECISION_WIN,
                                    80,
                                    LocalDateTime.of(2026, 7, 22, 12, 0),
                                ),
                            ),
                        nextCursor = transactionId,
                        hasNext = true,
                    ),
            ),
        )

        mockMvc.perform(
            get("/api/ap/transactions")
                .param("userId", userId.toString())
                .param("size", "1"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("COMMON_200"))
            .andExpect(jsonPath("$.result.summary.balanceAp").value(120))
            .andExpect(jsonPath("$.result.page.items[0].apTransactionId").value(transactionId.toString()))
            .andExpect(jsonPath("$.result.page.nextCursor").value(transactionId.toString()))
    }

    @Test
    fun `요청 파라미터의 userId는 인증 사용자 결정에 사용하지 않는다`() {
        mockMvc.perform(get("/api/ap/transactions"))
            .andExpect(status().isOk)
        mockMvc.perform(get("/api/ap/transactions").param("userId", "invalid"))
            .andExpect(status().isOk)
    }

    @Test
    fun `출석 보상은 AP 전용 성공 코드와 보상 분기를 반환한다`() {
        `when`(service.createAttendanceReward(userId)).thenReturn(
            CreateAttendanceRewardResponse(250, true, 7, 350),
        )

        mockMvc.perform(post("/api/ap/attendance-rewards").param("userId", userId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("AP_200_01"))
            .andExpect(jsonPath("$.result.rewardedAp").value(250))
            .andExpect(jsonPath("$.result.bonusRewarded").value(true))
            .andExpect(jsonPath("$.result.consecutiveDays").value(7))
    }

    @Test
    fun `중복 출석은 AP_409_01을 반환한다`() {
        `when`(service.createAttendanceReward(userId)).thenThrow(AttendanceRewardAlreadyClaimedException())

        mockMvc.perform(post("/api/ap/attendance-rewards").param("userId", userId.toString()))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("AP_409_01"))
    }

    @Test
    fun `튜토리얼과 신용대출은 각각 AP 성공 코드를 반환한다`() {
        val agentId = UUID.randomUUID()
        `when`(service.createTutorialReward(userId)).thenReturn(ApBalanceResponse(200))
        `when`(service.createCreditLoan(userId, CreateCreditLoanRequest(agentId))).thenReturn(ApBalanceResponse(400))

        mockMvc.perform(post("/api/ap/tutorial-rewards").param("userId", userId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("AP_200_02"))
            .andExpect(jsonPath("$.result.balanceAp").value(200))
        mockMvc.perform(
            post("/api/ap/credit-loans")
                .param("userId", userId.toString())
                .contentType("application/json")
                .content("""{"agentId":"$agentId"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("AP_200_03"))
            .andExpect(jsonPath("$.result.balanceAp").value(400))
    }

    @Test
    fun `신용대출 agentId 누락과 잘못된 UUID는 400이다`() {
        listOf("{}", """{"agentId":"invalid"}""").forEach { body ->
            mockMvc.perform(
                post("/api/ap/credit-loans")
                    .param("userId", userId.toString())
                    .contentType("application/json")
                    .content(body),
            ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("COMMON_400"))
        }
    }
}
