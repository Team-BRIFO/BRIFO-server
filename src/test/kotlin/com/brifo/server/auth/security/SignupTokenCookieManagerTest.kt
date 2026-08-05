package com.brifo.server.auth.security

import com.brifo.server.auth.config.SignupTokenCookieProperties
import com.brifo.server.global.config.JwtProperties
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignupTokenCookieManagerTest {
    private val manager =
        SignupTokenCookieManager(
            SignupTokenCookieProperties(secure = true),
            JwtProperties(
                secretBase64 = "unused",
                signupTokenExpiration = Duration.ofMinutes(10),
            ),
        )

    @Test
    fun `Signup Token을 보안 속성이 적용된 HttpOnly 쿠키로 설정한다`() {
        val response = MockHttpServletResponse()

        manager.set(response, "signup-token")

        val cookies = response.getHeaders(HttpHeaders.SET_COOKIE)
        val signupCookie = cookies.single { it.startsWith("${SignupTokenCookieManager.COOKIE_NAME}=") }
        val csrfCookie = cookies.single { it.startsWith("${SignupTokenCookieManager.CSRF_COOKIE_NAME}=") }

        assertTrue(signupCookie.startsWith("${SignupTokenCookieManager.COOKIE_NAME}=signup-token"))
        assertTrue(signupCookie.contains("Path=/api"))
        assertTrue(signupCookie.contains("Max-Age=600"))
        assertTrue(signupCookie.contains("Secure"))
        assertTrue(signupCookie.contains("HttpOnly"))
        assertTrue(signupCookie.contains("SameSite=Lax"))
        assertTrue(csrfCookie.contains("Path=/api"))
        assertTrue(csrfCookie.contains("Max-Age=600"))
        assertTrue(csrfCookie.contains("Secure"))
        assertTrue(csrfCookie.contains("SameSite=Lax"))
        assertTrue(csrfCookie.contains("HttpOnly"))
        assertEquals(
            csrfCookie.substringAfter('=').substringBefore(';'),
            response.getHeader(SignupTokenCookieManager.CSRF_HEADER_NAME),
        )
    }

    @Test
    fun `Signup Token 쿠키를 만료시켜 삭제한다`() {
        val response = MockHttpServletResponse()

        manager.clear(response)

        val cookies = response.getHeaders(HttpHeaders.SET_COOKIE)
        assertEquals(2, cookies.size)
        assertTrue(cookies.any { it.startsWith("${SignupTokenCookieManager.COOKIE_NAME}=") })
        assertTrue(cookies.any { it.startsWith("${SignupTokenCookieManager.CSRF_COOKIE_NAME}=") })
        assertTrue(cookies.all { it.contains("Max-Age=0") })
        assertTrue(cookies.all { it.contains("Path=/api") })
    }

    @Test
    fun `CSRF 토큰을 새 쿠키와 응답 헤더로 재발급한다`() {
        val request =
            MockHttpServletRequest().apply {
                setCookies(Cookie(SignupTokenCookieManager.COOKIE_NAME, "signup-token"))
            }
        val response = MockHttpServletResponse()
        val repeatedResponse = MockHttpServletResponse()

        manager.refreshCsrfToken(request, response)
        manager.refreshCsrfToken(request, repeatedResponse)

        val cookies = response.getHeaders(HttpHeaders.SET_COOKIE)
        val csrfCookie = cookies.single { it.startsWith("${SignupTokenCookieManager.CSRF_COOKIE_NAME}=") }
        assertTrue(csrfCookie.contains("HttpOnly"))
        assertEquals(
            csrfCookie.substringAfter('=').substringBefore(';'),
            response.getHeader(SignupTokenCookieManager.CSRF_HEADER_NAME),
        )
        assertEquals(
            response.getHeader(SignupTokenCookieManager.CSRF_HEADER_NAME),
            repeatedResponse.getHeader(SignupTokenCookieManager.CSRF_HEADER_NAME),
        )
    }

    @Test
    fun `요청 쿠키에서 Signup Token을 찾는다`() {
        val request =
            MockHttpServletRequest().apply {
                setCookies(Cookie("other", "value"), Cookie(SignupTokenCookieManager.COOKIE_NAME, "signup-token"))
            }

        assertEquals("signup-token", manager.resolve(request))
    }

    @Test
    fun `CSRF 쿠키와 헤더의 토큰이 같으면 검증에 성공한다`() {
        val request =
            MockHttpServletRequest().apply {
                setCookies(Cookie(SignupTokenCookieManager.CSRF_COOKIE_NAME, "csrf-token"))
                addHeader(SignupTokenCookieManager.CSRF_HEADER_NAME, "csrf-token")
            }

        assertTrue(manager.matchesCsrfToken(request))
    }

    @Test
    fun `CSRF 쿠키와 헤더의 토큰이 다르면 검증에 실패한다`() {
        val request =
            MockHttpServletRequest().apply {
                setCookies(Cookie(SignupTokenCookieManager.CSRF_COOKIE_NAME, "cookie-token"))
                addHeader(SignupTokenCookieManager.CSRF_HEADER_NAME, "header-token")
            }

        assertFalse(manager.matchesCsrfToken(request))
    }
}
