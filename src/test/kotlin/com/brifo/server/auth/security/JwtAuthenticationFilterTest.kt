package com.brifo.server.auth.security

import com.brifo.server.auth.code.AuthErrorCode
import com.brifo.server.auth.exception.InvalidTokenException
import com.brifo.server.auth.service.JwtTokenProvider
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class JwtAuthenticationFilterTest {
    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var filter: JwtAuthenticationFilter

    @BeforeEach
    fun setUp() {
        filter = JwtAuthenticationFilter(jwtTokenProvider)
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `유효한 Access Token은 사용자 공개 ID로 인증한다`() {
        val userId = UUID.randomUUID()
        val request = MockHttpServletRequest().apply { addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token") }
        val response = MockHttpServletResponse()
        var authentication: Authentication? = null
        val filterChain = FilterChain { _, _ -> authentication = SecurityContextHolder.getContext().authentication }
        `when`(jwtTokenProvider.parseAccessToken("access-token"))
            .thenReturn(JwtTokenProvider.AuthTokenClaims(userId, "token-id", Instant.now().plusSeconds(60)))

        filter.doFilter(request, response, filterChain)

        assertEquals(userId, authentication?.principal)
        assertEquals(true, authentication?.isAuthenticated)
    }

    @Test
    fun `유효하지 않은 Access Token은 인증하지 않고 오류 코드를 보존한다`() {
        val request = MockHttpServletRequest().apply { addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token") }
        val response = MockHttpServletResponse()
        val filterChain = FilterChain { _, _ -> }
        `when`(jwtTokenProvider.parseAccessToken("invalid-token")).thenThrow(InvalidTokenException())

        filter.doFilter(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        assertSame(
            AuthErrorCode.OAUTH_INVALID_TOKEN,
            request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_CODE_ATTRIBUTE),
        )
    }

    @Test
    fun `Bearer 형식이 아닌 Authorization 헤더는 유효하지 않은 토큰으로 처리한다`() {
        val request = MockHttpServletRequest().apply { addHeader(HttpHeaders.AUTHORIZATION, "Basic credentials") }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, FilterChain { _, _ -> })

        assertNull(SecurityContextHolder.getContext().authentication)
        assertSame(
            AuthErrorCode.OAUTH_INVALID_TOKEN,
            request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_CODE_ATTRIBUTE),
        )
    }
}
