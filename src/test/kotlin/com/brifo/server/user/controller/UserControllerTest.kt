package com.brifo.server.user.controller

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.news.entity.NewsSource
import com.brifo.server.user.dto.request.UpdateOnboardingProfileRequest
import com.brifo.server.user.dto.request.UpdateUserProfileRequest
import com.brifo.server.user.dto.response.GetMyPageResponse
import com.brifo.server.user.dto.response.GetUserHomeResponse
import com.brifo.server.user.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(UserController::class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userService: UserService

    @Test
    fun `온보딩 프로필 저장은 임시 사용자 ID를 받아 성공 응답을 반환한다`() {
        val userId = UUID.randomUUID()
        val request =
            UpdateOnboardingProfileRequest(
                nickname = "brifo",
                companyName = "내 투자회사",
            )

        mockMvc
            .perform(
                patch("/api/onboarding/profile")
                    .param("userId", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "nickname": "${request.nickname}",
                          "companyName": "${request.companyName}"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("USER_200_01"))
            .andExpect(jsonPath("$.message").value("기본 정보가 저장되었습니다."))
            .andExpect(jsonPath("$.result").doesNotExist())

        verify(userService).updateOnboardingProfile(userId, request)
    }

    @Test
    fun `온보딩 프로필 저장에 사용자 ID가 없으면 400을 반환한다`() {
        mockMvc
            .perform(
                patch("/api/onboarding/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "nickname": "brifo"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("COMMON_400"))
    }

    @Test
    fun `온보딩 완료는 임시 사용자 ID를 받아 사용자 처리를 완료한다`() {
        val userId = UUID.randomUUID()

        mockMvc
            .perform(
                post("/api/onboarding/complete")
                    .param("userId", userId.toString()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("USER_200_02"))
            .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
            .andExpect(jsonPath("$.result").doesNotExist())

        verify(userService).completeOnboarding(userId)
    }

    @Test
    fun `온보딩 완료에 사용자 ID가 없으면 400을 반환한다`() {
        mockMvc
            .perform(post("/api/onboarding/complete"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("COMMON_400"))
    }

    @Test
    fun `마이페이지 조회는 임시 사용자 ID로 집계 결과를 반환한다`() {
        val userId = UUID.randomUUID()
        val response =
            GetMyPageResponse(
                nickname = "brifo",
                companyName = "내 투자회사",
                balanceAp = 1250,
                thisWeekEarnedAp = 450,
                decisionAccuracyRate = 63,
                totalDecision = 48,
                consecutiveDays = 5,
                learnedTermCount = 24,
            )
        `when`(userService.getMyPage(userId)).thenReturn(response)

        mockMvc
            .perform(
                get("/api/users/me")
                    .param("userId", userId.toString()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("COMMON_200"))
            .andExpect(jsonPath("$.result.nickname").value("brifo"))
            .andExpect(jsonPath("$.result.companyName").value("내 투자회사"))
            .andExpect(jsonPath("$.result.balanceAp").value(1250))
            .andExpect(jsonPath("$.result.thisWeekEarnedAp").value(450))
            .andExpect(jsonPath("$.result.decisionAccuracyRate").value(63))
            .andExpect(jsonPath("$.result.totalDecision").value(48))
            .andExpect(jsonPath("$.result.consecutiveDays").value(5))
            .andExpect(jsonPath("$.result.learnedTermCount").value(24))

        verify(userService).getMyPage(userId)
    }

    @Test
    fun `마이페이지 조회에 사용자 ID가 없으면 400을 반환한다`() {
        mockMvc
            .perform(get("/api/users/me"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("COMMON_400"))
    }

    @Test
    fun `홈 화면 조회는 임시 사용자 ID로 대시보드 정보를 반환한다`() {
        val userId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        val cardId = UUID.randomUUID()
        val newsId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        val batchTime = LocalDateTime.of(2026, 7, 21, 8, 0)
        val publishedAt = LocalDateTime.of(2026, 7, 21, 7, 30)
        val response =
            GetUserHomeResponse(
                user = GetUserHomeResponse.User("brifo", "내 투자회사", 1250),
                agents = listOf(GetUserHomeResponse.Agent(agentId, AgentType.ROOKIE, 3)),
                todayDecisions = GetUserHomeResponse.TodayDecisions(2),
                todayNewsCards =
                    GetUserHomeResponse.TodayNewsCards(
                        batchTime = batchTime,
                        items =
                            listOf(
                                GetUserHomeResponse.TodayNewsCards.Item(
                                    cardId = cardId,
                                    headline = "삼성전자, 반도체 실적 개선 기대",
                                    news =
                                        GetUserHomeResponse.TodayNewsCards.News(
                                            newsId,
                                            publishedAt,
                                            NewsSource.NAVER,
                                        ),
                                    stock =
                                        GetUserHomeResponse.TodayNewsCards.Stock(
                                            stockId,
                                            "삼성전자",
                                            BigDecimal("1.3"),
                                        ),
                                ),
                            ),
                    ),
            )
        `when`(userService.getUserHome(userId)).thenReturn(response)

        mockMvc
            .perform(
                get("/api/users/me/home")
                    .param("userId", userId.toString()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("COMMON_200"))
            .andExpect(jsonPath("$.result.user.nickname").value("brifo"))
            .andExpect(jsonPath("$.result.user.balanceAp").value(1250))
            .andExpect(jsonPath("$.result.agents[0].agentId").value(agentId.toString()))
            .andExpect(jsonPath("$.result.agents[0].agentType").value("ROOKIE"))
            .andExpect(jsonPath("$.result.todayDecisions.count").value(2))
            .andExpect(jsonPath("$.result.todayNewsCards.batchTime").value("2026-07-21T08:00:00"))
            .andExpect(jsonPath("$.result.todayNewsCards.items[0].cardId").value(cardId.toString()))
            .andExpect(jsonPath("$.result.todayNewsCards.items[0].news.newsId").value(newsId.toString()))
            .andExpect(jsonPath("$.result.todayNewsCards.items[0].stock.stockId").value(stockId.toString()))
            .andExpect(jsonPath("$.result.todayNewsCards.items[0].stock.changeRate").value(1.3))

        verify(userService).getUserHome(userId)
    }

    @Test
    fun `홈 화면 조회에 사용자 ID가 없으면 400을 반환한다`() {
        mockMvc
            .perform(get("/api/users/me/home"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("COMMON_400"))
    }

    @Test
    fun `프로필 수정은 임시 사용자 ID와 전체 프로필 정보를 전달한다`() {
        val userId = UUID.randomUUID()
        val stockIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val request =
            UpdateUserProfileRequest(
                nickname = "brifo",
                companyName = "BRIFO 투자회사",
                stockIds = stockIds,
            )

        mockMvc
            .perform(
                patch("/api/users/me/profile")
                    .param("userId", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "nickname": "${request.nickname}",
                          "companyName": "${request.companyName}",
                          "stockIds": ["${stockIds[0]}", "${stockIds[1]}"]
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("USER_200_03"))
            .andExpect(jsonPath("$.message").value("프로필이 수정되었습니다."))
            .andExpect(jsonPath("$.result").doesNotExist())

        verify(userService).updateUserProfile(userId, request)
    }

    @Test
    fun `프로필 수정에 사용자 ID가 없으면 400을 반환한다`() {
        mockMvc
            .perform(
                patch("/api/users/me/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "nickname": "brifo",
                          "companyName": "BRIFO 투자회사",
                          "stockIds": ["${UUID.randomUUID()}"]
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("COMMON_400"))
    }
}
