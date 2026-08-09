package com.brifo.server.auth.security

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignupCsrfFilterTest {
    private lateinit var signupTokenCookieManager: SignupTokenCookieManager

    @BeforeEach
    fun setUp() {
        signupTokenCookieManager = mock(SignupTokenCookieManager::class.java)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                UUID.randomUUID(),
                null,
                listOf(SimpleGrantedAuthority(JwtAuthenticationFilter.SIGNUP_AUTHORITY)),
            )
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `Signup 인증 요청의 CSRF 토큰이 일치하면 요청을 전달한다`() {
        val request = MockHttpServletRequest("PATCH", "/api/onboarding/profile")
        val response = MockHttpServletResponse()
        var filterChainCalled = false
        var accessDenied = false
        val filter =
            SignupCsrfFilter(signupTokenCookieManager) { _, _, _ ->
                accessDenied = true
            }
        val filterChain = FilterChain { _, _ -> filterChainCalled = true }
        `when`(signupTokenCookieManager.matchesCsrfToken(request)).thenReturn(true)

        filter.doFilter(request, response, filterChain)

        assertTrue(filterChainCalled)
        assertFalse(accessDenied)
    }

    @Test
    fun `Signup 인증 요청의 CSRF 검증이 실패하면 요청을 거부한다`() {
        val request = MockHttpServletRequest("POST", "/api/onboarding/complete")
        val response = MockHttpServletResponse()
        var filterChainCalled = false
        var accessDenied = false
        val filter =
            SignupCsrfFilter(signupTokenCookieManager) { _, _, _ ->
                accessDenied = true
            }
        val filterChain = FilterChain { _, _ -> filterChainCalled = true }
        `when`(signupTokenCookieManager.matchesCsrfToken(request)).thenReturn(false)

        filter.doFilter(request, response, filterChain)

        assertFalse(filterChainCalled)
        assertTrue(accessDenied)
    }

    @Test
    fun `Signup 인증의 안전한 조회 요청은 CSRF 검증 없이 전달한다`() {
        val request = MockHttpServletRequest("GET", "/api/policies")
        val response = MockHttpServletResponse()
        var filterChainCalled = false
        var accessDenied = false
        val filter =
            SignupCsrfFilter(signupTokenCookieManager) { _, _, _ ->
                accessDenied = true
            }
        val filterChain = FilterChain { _, _ -> filterChainCalled = true }

        filter.doFilter(request, response, filterChain)

        assertTrue(filterChainCalled)
        assertFalse(accessDenied)
    }

    @Test
    fun `개발용 회원가입 POST 요청은 CSRF 검증 없이 전달한다`() {
        val request = MockHttpServletRequest("POST", "/api/dev/signup")
        val response = MockHttpServletResponse()
        var filterChainCalled = false
        var accessDenied = false
        val filter = SignupCsrfFilter(signupTokenCookieManager) { _, _, _ -> accessDenied = true }

        filter.doFilter(request, response) { _, _ -> filterChainCalled = true }

        assertTrue(filterChainCalled)
        assertFalse(accessDenied)
    }

    @Test
    fun `개발용 회원가입 경로의 POST 외 변경 요청은 CSRF 보호를 받는다`() {
        val request = MockHttpServletRequest("PATCH", "/api/dev/signup")
        val response = MockHttpServletResponse()
        var filterChainCalled = false
        var accessDenied = false
        val filter = SignupCsrfFilter(signupTokenCookieManager) { _, _, _ -> accessDenied = true }
        `when`(signupTokenCookieManager.matchesCsrfToken(request)).thenReturn(false)

        filter.doFilter(request, response) { _, _ -> filterChainCalled = true }

        assertTrue(accessDenied)
        assertFalse(filterChainCalled)
    }

}
