package com.brifo.server.user.controller

import com.brifo.server.user.dto.request.UpdateOnboardingProfileRequest
import com.brifo.server.user.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
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
}
