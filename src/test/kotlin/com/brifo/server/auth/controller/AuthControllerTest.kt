package com.brifo.server.auth.controller

import com.brifo.server.auth.dto.request.KakaoLoginRequest
import com.brifo.server.auth.dto.request.RefreshTokenRequest
import com.brifo.server.auth.dto.response.OAuthLoginResponse
import com.brifo.server.auth.dto.response.ReissueResponse
import com.brifo.server.auth.dto.response.TokenInfo
import com.brifo.server.auth.security.SignupTokenCookieManager
import com.brifo.server.auth.service.KakaoLoginService
import com.brifo.server.auth.service.LogoutService
import com.brifo.server.auth.service.NaverLoginService
import com.brifo.server.auth.service.TokenReissueService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.UUID

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

    @Mock
    private lateinit var signupTokenCookieManager: SignupTokenCookieManager

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(
                    AuthController(
                        kakaoLoginService,
                        naverLoginService,
                        logoutService,
                        tokenReissueService,
                        signupTokenCookieManager,
                    ),
                ).setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
                .build()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `로그아웃은 인증 사용자와 Refresh Token을 검증하고 성공 응답을 반환한다`() {
        val userPublicId = UUID.randomUUID()
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userPublicId, null, emptyList())

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

        verify(logoutService).logout(userPublicId, RefreshTokenRequest("refresh-token"))
    }

    @Test
    fun `Refresh Token으로 새 토큰 쌍을 발급한다`() {
        val tokens = TokenInfo("new-access-token", "new-refresh-token", 3600, 604800)
        org.mockito.Mockito
            .`when`(tokenReissueService.reissue(RefreshTokenRequest("refresh-token")))
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

    @Test
    fun `온보딩이 필요하면 Signup Token을 응답에서 숨기고 쿠키로 전달한다`() {
        val request = KakaoLoginRequest("authorization-code", "http://localhost:3000/oauth/callback/kakao")
        val result = OAuthLoginResponse.SignupRequired(signupToken = "signup-token")
        `when`(kakaoLoginService.login(request)).thenReturn(result)

        mockMvc
            .perform(
                post("/api/auth/login/kakao")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"authorizationCode":"authorization-code","redirectUri":"http://localhost:3000/oauth/callback/kakao"}""",
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.result.loginType").value("SIGNUP_REQUIRED"))
            .andExpect(jsonPath("$.result.signupToken").doesNotExist())

        val cookieInvocation =
            mockingDetails(signupTokenCookieManager).invocations.single { it.method.name == "set" }
        org.junit.jupiter.api.Assertions
            .assertEquals("signup-token", cookieInvocation.arguments[1])
    }
}
