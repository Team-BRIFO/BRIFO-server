package com.brifo.server.auth.controller

import com.brifo.server.auth.dto.request.LogoutRequest
import com.brifo.server.auth.dto.request.ReissueRequest
import com.brifo.server.auth.dto.response.ReissueResponse
import com.brifo.server.auth.dto.response.TokenInfo
import com.brifo.server.auth.service.KakaoLoginService
import com.brifo.server.auth.service.LogoutService
import com.brifo.server.auth.service.NaverLoginService
import com.brifo.server.auth.service.TokenReissueService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@ExtendWith(MockitoExtension::class)
class AuthControllerTest {
    @Mock
    private lateinit var kakaoLoginService: KakaoLoginService

    @Mock
    private lateinit var naverLoginService: NaverLoginService

    @Mock
    private lateinit var logoutService: LogoutService

    @Mock
    private lateinit var tokenReissueService: TokenReissueService

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(AuthController(kakaoLoginService, naverLoginService, logoutService, tokenReissueService))
                .build()
    }

    @Test
    fun `로그아웃은 Access Token과 Refresh Token을 검증하고 성공 응답을 반환한다`() {
        mockMvc
            .perform(
                post("/api/auth/logout")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"refreshToken":"refresh-token"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("COMMON_200"))
            .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
            .andExpect(jsonPath("$.result").doesNotExist())

        verify(logoutService).logout("Bearer access-token", LogoutRequest("refresh-token"))
    }

    @Test
    fun `Refresh Token으로 새 토큰 쌍을 발급한다`() {
        val tokens = TokenInfo("new-access-token", "new-refresh-token", 3600, 604800)
        org.mockito.Mockito
            .`when`(tokenReissueService.reissue(ReissueRequest("refresh-token")))
            .thenReturn(ReissueResponse(tokens))

        mockMvc
            .perform(
                post("/api/auth/reissue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"refreshToken":"refresh-token"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("COMMON_200"))
            .andExpect(jsonPath("$.result.token.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.result.token.refreshToken").value("new-refresh-token"))
            .andExpect(jsonPath("$.result.token.accessTokenExpiresIn").value(3600))
            .andExpect(jsonPath("$.result.token.refreshTokenExpiresIn").value(604800))
    }
}
