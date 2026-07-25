package com.brifo.server.auth.security

import com.brifo.server.ServerTestConfiguration
import com.brifo.server.auth.service.JwtTokenProvider
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@Import(ServerTestConfiguration::class)
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @Test
    fun `보호 API는 Access Token이 없으면 인증 오류를 반환한다`() {
        mockMvc
            .perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_401"))
    }

    @Test
    fun `유효하지 않은 Access Token은 토큰 오류를 반환한다`() {
        mockMvc
            .perform(
                get("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_401_03"))
    }

    @Test
    fun `토큰 재발급 API는 인증 없이 호출할 수 있다`() {
        mockMvc
            .perform(
                post("/api/auth/reissue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("AUTH_400_02"))
    }

    @Test
    fun `카카오 로그인 API는 인증 없이 호출할 수 있다`() {
        mockMvc
            .perform(
                post("/api/auth/login/kakao")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `네이버 로그인 API는 인증 없이 호출할 수 있다`() {
        mockMvc
            .perform(
                post("/api/auth/login/naver")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `구현되지 않은 로그인 경로는 공개하지 않는다`() {
        mockMvc
            .perform(
                post("/api/auth/login/unknown")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_401"))
    }

    @Test
    fun `토큰 재발급 경로는 POST 요청만 공개한다`() {
        mockMvc
            .perform(get("/api/auth/reissue"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_401"))
    }

    @Test
    fun `로그아웃 API는 Access Token이 없으면 인증 오류를 반환한다`() {
        mockMvc
            .perform(
                post("/api/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"refreshToken":"refresh-token"}"""),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_401"))
    }

    @Test
    fun `유효한 Access Token은 로그아웃 API 인증을 통과한다`() {
        val accessToken = jwtTokenProvider.issueLoginTokens(UUID.randomUUID()).accessToken

        mockMvc
            .perform(
                post("/api/auth/logout")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("AUTH_400_02"))
    }
}
