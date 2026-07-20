package com.brifo.server.auth.service

import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.config.KakaoProperties
import com.brifo.server.global.exception.BusinessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest
import org.springframework.web.client.RestClient
import tools.jackson.module.kotlin.jacksonObjectMapper

class KakaoApiClientTest {
    private lateinit var server: MockRestServiceServer
    private lateinit var client: KakaoApiClient

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder()
        server = MockRestServiceServer.bindTo(builder).build()
        client = KakaoApiClient(builder.build(), properties(), jacksonObjectMapper())
    }

    @Test
    fun `인가 코드로 토큰을 교환하고 카카오 사용자를 조회한다`() {
        server
            .expect(requestTo("https://kauth.kakao.com/oauth/token"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("""{"access_token":"kakao-access-token"}""", MediaType.APPLICATION_JSON))
        server
            .expect(requestTo("https://kapi.kakao.com/v2/user/me"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """{"id":1234567890,"kakao_account":{"email":"user@kakao.com","profile":{"nickname":"brifo"}}}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val user = client.getUser("authorization-code", REDIRECT_URI)

        assertEquals(1234567890L, user.id)
        assertEquals("user@kakao.com", user.kakaoAccount?.email)
        assertEquals("brifo", user.kakaoAccount?.profile?.nickname)
        server.verify()
    }

    @Test
    fun `등록되지 않은 Redirect URI는 카카오 호출 전에 거부한다`() {
        val exception =
            assertThrows(BusinessException::class.java) {
                client.getUser("authorization-code", "https://attacker.example/callback")
            }

        assertEquals(ErrorCode.OAUTH_REDIRECT_URI_MISMATCH, exception.errorCode)
        server.verify()
    }

    @Test
    fun `만료된 인가 코드는 AUTH4011로 변환한다`() {
        server
            .expect(requestTo("https://kauth.kakao.com/oauth/token"))
            .andRespond(
                withBadRequest().body(
                    """{"error":"invalid_grant","error_description":"authorization code not found"}""",
                ),
            )

        val exception =
            assertThrows(BusinessException::class.java) {
                client.getUser("expired-code", REDIRECT_URI)
            }

        assertEquals(ErrorCode.OAUTH_INVALID_AUTHORIZATION_CODE, exception.errorCode)
        server.verify()
    }

    @Test
    fun `카카오 토큰 오류는 AUTH4013으로 변환한다`() {
        server
            .expect(requestTo("https://kauth.kakao.com/oauth/token"))
            .andRespond(withSuccess("""{"access_token":"invalid-token"}""", MediaType.APPLICATION_JSON))
        server
            .expect(requestTo("https://kapi.kakao.com/v2/user/me"))
            .andRespond(withUnauthorizedRequest().body("""{"code":-401,"msg":"invalid token"}"""))

        val exception =
            assertThrows(BusinessException::class.java) {
                client.getUser("authorization-code", REDIRECT_URI)
            }

        assertEquals(ErrorCode.OAUTH_INVALID_TOKEN, exception.errorCode)
        server.verify()
    }

    private fun properties() =
        KakaoProperties(
            clientId = "test-client-id",
            clientSecret = "test-client-secret",
            redirectUris = setOf(REDIRECT_URI),
        )

    companion object {
        private const val REDIRECT_URI = "http://localhost:3000/oauth/callback/kakao"
    }
}
