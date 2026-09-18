package com.brifo.server.diary.controller

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.authenticatedUserId
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.diary.dto.response.CreateDiaryShareImageResponse
import com.brifo.server.diary.dto.response.GetDiaryDayDetailResponse
import com.brifo.server.diary.service.DiaryService
import com.brifo.server.diary.service.DiaryShareImageService
import com.brifo.server.global.error.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.env.MockEnvironment
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class DiaryControllerTest {
    private val service = mock(DiaryService::class.java)
    private val shareImageService = mock(DiaryShareImageService::class.java)
    private val userId = authenticatedUserId()
    private val mockMvc =
        run {
            val validator = LocalValidatorFactoryBean().also { it.afterPropertiesSet() }
            MockMvcBuilders
                .standaloneSetup(DiaryController(service, shareImageService))
                .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(GlobalExceptionHandler(MockEnvironment()))
                .setValidator(validator)
                .build()
        }

    @Test
    fun `목록 크기가 범위를 벗어나면 400을 반환한다`() {
        mockMvc
            .perform(
                get("/api/diaries")
                    .param("userId", userId.toString())
                    .param("size", "51"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))
    }

    @Test
    fun `캘린더 월이 범위를 벗어나면 400을 반환한다`() {
        mockMvc
            .perform(
                get("/api/diaries/calendar")
                    .param("userId", userId.toString())
                    .param("year", "2026")
                    .param("month", "13"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))
    }

    @Test
    fun `공유 이미지를 생성하면 URL과 재사용 여부를 반환한다`() {
        val diaryId = UUID.randomUUID()
        val imageUrl = "https://cdn.brifo.app/diary-share-images/$diaryId.png"
        `when`(shareImageService.create(userId, diaryId)).thenReturn(
            CreateDiaryShareImageResponse(
                diaryId = diaryId,
                shareImageUrl = imageUrl,
                reused = false,
                changeRate = BigDecimal("2.55"),
                tradeDate = LocalDate.of(2026, 8, 10),
            ),
        )

        mockMvc
            .perform(post("/api/diaries/{diaryId}/share-images", diaryId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("DIARY_200_01"))
            .andExpect(jsonPath("$.result.diaryId").value(diaryId.toString()))
            .andExpect(jsonPath("$.result.shareImageUrl").value(imageUrl))
            .andExpect(jsonPath("$.result.reused").value(false))
    }

    @Test
    fun `날짜별 상세는 그날의 결정 목록을 반환한다`() {
        val date = LocalDate.of(2026, 7, 21)
        val diaryId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        `when`(service.getDiaryDayDetail(userId, date)).thenReturn(
            GetDiaryDayDetailResponse(
                date = date,
                items = listOf(
                    GetDiaryDayDetailResponse.DayDetailItem(
                        diaryId = diaryId,
                        stock = GetDiaryDayDetailResponse.DayDetailStock(
                            stockId = stockId,
                            name = "삼성전자",
                            logoUrl = null,
                            changeRate = BigDecimal("2.6"),
                        ),
                        agent = GetDiaryDayDetailResponse.DayDetailAgent(
                            agentId = agentId,
                            agentType = AgentType.ROOKIE,
                            nickname = "루키",
                        ),
                        decision = GetDiaryDayDetailResponse.DayDetailDecision(
                            direction = DecisionDirection.UP,
                            confidenceLevel = 4,
                            isCorrect = true,
                            apDelta = 80_000,
                        ),
                    ),
                ),
            ),
        )

        mockMvc
            .perform(get("/api/diaries/calendar/{date}", date).param("userId", userId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.date").value("2026-07-21"))
            .andExpect(jsonPath("$.result.items[0].diaryId").value(diaryId.toString()))
            .andExpect(jsonPath("$.result.items[0].stock.name").value("삼성전자"))
            .andExpect(jsonPath("$.result.items[0].agent.nickname").value("루키"))
            .andExpect(jsonPath("$.result.items[0].decision.isCorrect").value(true))
    }
}
