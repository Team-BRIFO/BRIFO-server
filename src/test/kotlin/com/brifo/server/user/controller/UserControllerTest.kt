package com.brifo.server.user.controller

import com.brifo.server.authenticatedUserId
import com.brifo.server.user.dto.request.UpdateOnboardingProfileRequest
import com.brifo.server.user.dto.request.UpdateUserProfileRequest
import com.brifo.server.user.dto.response.CompleteOnboardingResponse
import com.brifo.server.user.dto.response.GetMyPageResponse
import com.brifo.server.user.dto.response.GetUserHomeResponse
import com.brifo.server.user.dto.response.GetUserProfileResponse
import com.brifo.server.user.service.UserQueryService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(UserController::class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userService: UserService

    @MockitoBean
    private lateinit var userQueryService: UserQueryService

    @Test
    fun `온보딩 프로필 요청을 서비스에 전달한다`() {
        val userId = authenticatedUserId()
        val stockId = UUID.randomUUID()
        val request = UpdateOnboardingProfileRequest("brifo", "회사", listOf(stockId))

        mockMvc
            .perform(
                patch("/api/onboarding/profile")
                    .param("userId", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"nickname":"brifo","companyName":"회사","stockIds":["$stockId"]}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("USER_200_01"))

        verify(userService).updateOnboardingProfile(userId, request)
    }

    @Test
    fun `온보딩 완료 요청을 서비스에 전달한다`() {
        val userId = authenticatedUserId()
        val response =
            CompleteOnboardingResponse(
                CompleteOnboardingResponse.Token("access-token", "refresh-token", 3_600, 604_800),
            )
        `when`(userService.completeOnboarding(userId)).thenReturn(response)

        mockMvc
            .perform(post("/api/onboarding/complete").param("userId", userId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("USER_200_02"))
            .andExpect(jsonPath("$.result").exists())

        verify(userService).completeOnboarding(userId)
    }

    @Test
    fun `마이페이지 응답을 직렬화한다`() {
        val userId = authenticatedUserId()
        val stockId = UUID.randomUUID()
        `when`(userQueryService.getMyPage(userId)).thenReturn(
            GetMyPageResponse(
                "brifo",
                "회사",
                1_250,
                450,
                67,
                48,
                5,
                24,
                listOf(GetMyPageResponse.MyPageStock(stockId, "삼성전자")),
            ),
        )

        mockMvc
            .perform(get("/api/users/me").param("userId", userId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("COMMON_200"))
            .andExpect(jsonPath("$.result.nickname").value("brifo"))
            .andExpect(jsonPath("$.result.decisionAccuracyRate").value(67))
            .andExpect(jsonPath("$.result.stocks[0].stockId").value(stockId.toString()))
            .andExpect(jsonPath("$.result.stocks[0].name").value("삼성전자"))

        verify(userQueryService).getMyPage(userId)
    }

    @Test
    fun `홈 조회 응답을 직렬화하고 조회 서비스에 전달한다`() {
        val userId = authenticatedUserId()
        `when`(userQueryService.getUserHome(userId)).thenReturn(
            GetUserHomeResponse(
                user = GetUserHomeResponse.User("brifo", "회사", 0),
                agents = emptyList(),
                attendedToday = true,
                weeklyAttendanceDays = 2,
                dates = listOf(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 4)),
                todayDecisions = GetUserHomeResponse.TodayDecisions(0),
                todayNewsCards = GetUserHomeResponse.TodayNewsCards(null, emptyList()),
            ),
        )

        mockMvc
            .perform(get("/api/users/me/home").param("userId", userId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("COMMON_200"))
            .andExpect(jsonPath("$.result.attendedToday").value(true))
            .andExpect(jsonPath("$.result.weeklyAttendanceDays").value(2))
            .andExpect(jsonPath("$.result.dates[0]").value("2026-08-03"))
            .andExpect(jsonPath("$.result.dates[1]").value("2026-08-04"))

        verify(userQueryService).getUserHome(userId)
    }

    @Test
    fun `프로필 조회 응답을 직렬화한다`() {
        val userId = authenticatedUserId()
        val stockId = UUID.randomUUID()
        `when`(userQueryService.getUserProfile(userId)).thenReturn(
            GetUserProfileResponse(
                nickname = "brifo",
                companyName = "회사",
                stocks = listOf(GetUserProfileResponse.UserProfileStock(stockId, "삼성전자")),
            ),
        )

        mockMvc
            .perform(get("/api/users/me/profile").param("userId", userId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.stocks[0].stockId").value(stockId.toString()))
            .andExpect(jsonPath("$.result.stocks[0].name").value("삼성전자"))

        verify(userQueryService).getUserProfile(userId)
    }

    @Test
    fun `프로필 수정 요청을 서비스에 전달한다`() {
        val userId = authenticatedUserId()
        val stockId = UUID.randomUUID()
        val request = UpdateUserProfileRequest("brifo", "회사", listOf(stockId))

        mockMvc
            .perform(
                patch("/api/users/me/profile")
                    .param("userId", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"nickname":"brifo","companyName":"회사","stockIds":["$stockId"]}""",
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("USER_200_03"))

        verify(userService).updateUserProfile(userId, request)
    }

    @Test
    fun `회원 탈퇴 요청을 서비스에 전달한다`() {
        val userId = authenticatedUserId()

        mockMvc
            .perform(delete("/api/users/me").param("userId", userId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("USER_200_04"))

        verify(userService).deleteUser(userId)
    }

    @Test
    fun `요청 파라미터의 userId는 인증 사용자 결정에 사용하지 않는다`() {
        authenticatedUserId()
        val stockId = UUID.randomUUID()
        mockMvc
            .perform(
                patch("/api/users/me/profile")
                    .param("userId", "invalid-uuid")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"nickname":"brifo","companyName":"회사","stockIds":["$stockId"]}"""),
            ).andExpect(status().isOk)
    }
}
