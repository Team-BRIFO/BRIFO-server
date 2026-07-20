package com.brifo.server.auth.service

import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.config.NaverProperties
import com.brifo.server.global.exception.BusinessException
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import tools.jackson.module.kotlin.jacksonObjectMapper

class NaverApiClientTest {
    private lateinit var server: MockRestServiceServer
    private lateinit var client: NaverApiClient

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder()
        server = MockRestServiceServer.bindTo(builder).build()
        client = NaverApiClient(builder.build(), properties(), jacksonObjectMapper())
    }

    @Test
    fun `인가 코드와 state로 토큰을 교환하고 네이버 사용자를 조회한다`() {
        server
            .expect(requestTo("https://nid.naver.com/oauth2.0/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(containsString("state=state-value")))
            .andRespond(withSuccess("""{"access_token":"naver-access-token"}""", MediaType.APPLICATION_JSON))
        server
            .expect(requestTo("https://openapi.naver.com/v1/nid/me"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer naver-access-token"))
            .andRespond(
                withSuccess(
                    """{"resultcode":"00","message":"success","response":{"id":"naver-id","email":"user@naver.com","nickname":"brifo"}}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val user = client.getUser("authorization-code", "state-value", REDIRECT_URI)

        assertEquals("naver-id", user.id)
        assertEquals("user@naver.com", user.email)
        assertEquals("brifo", user.nickname)
        server.verify()
    }

    @Test
    fun `등록되지 않은 Redirect URI는 네이버 호출 전에 거부한다`() {
        val exception =
            assertThrows(BusinessException::class.java) {
                client.getUser("authorization-code", "state-value", "https://attacker.example/callback")
            }

        assertEquals(ErrorCode.OAUTH_REDIRECT_URI_MISMATCH, exception.errorCode)
        server.verify()
    }

    @Test
    fun `만료된 인가 코드는 AUTH_401_01로 변환한다`() {
        server
            .expect(requestTo("https://nid.naver.com/oauth2.0/token"))
            .andRespond(
                withSuccess(
                    """{"error":"invalid_grant","error_description":"authorization code is expired"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val exception =
            assertThrows(BusinessException::class.java) {
                client.getUser("expired-code", "state-value", REDIRECT_URI)
            }

        assertEquals(ErrorCode.OAUTH_INVALID_AUTHORIZATION_CODE, exception.errorCode)
        server.verify()
    }

    @Test
    fun `네이버 프로필 인증 오류는 AUTH_401_03으로 변환한다`() {
        server
            .expect(requestTo("https://nid.naver.com/oauth2.0/token"))
            .andRespond(withSuccess("""{"access_token":"invalid-token"}""", MediaType.APPLICATION_JSON))
        server
            .expect(requestTo("https://openapi.naver.com/v1/nid/me"))
            .andRespond(
                withSuccess(
                    """{"resultcode":"024","message":"Authentication failed"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val exception =
            assertThrows(BusinessException::class.java) {
                client.getUser("authorization-code", "state-value", REDIRECT_URI)
            }

        assertEquals(ErrorCode.OAUTH_INVALID_TOKEN, exception.errorCode)
        server.verify()
    }

    @Test
    fun `네이버 클라이언트 인증 오류는 AUTH_502로 변환한다`() {
        server
            .expect(requestTo("https://nid.naver.com/oauth2.0/token"))
            .andRespond(
                withSuccess(
                    """{"error":"invalid_client","error_description":"client authentication failed"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val exception =
            assertThrows(BusinessException::class.java) {
                client.getUser("authorization-code", "state-value", REDIRECT_URI)
            }

        assertEquals(ErrorCode.NAVER_SERVER_ERROR, exception.errorCode)
        server.verify()
    }

    private fun properties() =
        NaverProperties(
            clientId = "test-client-id",
            clientSecret = "test-client-secret",
            redirectUris = setOf(REDIRECT_URI),
        )

    companion object {
        private const val REDIRECT_URI = "http://localhost:3000/oauth/callback/naver"
    }
}
