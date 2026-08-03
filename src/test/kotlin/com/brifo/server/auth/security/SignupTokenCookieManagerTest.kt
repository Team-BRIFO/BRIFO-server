package com.brifo.server.auth.security

import com.brifo.server.auth.config.SignupTokenCookieProperties
import com.brifo.server.global.config.JwtProperties
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Duration

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

        val setCookie = response.getHeader(HttpHeaders.SET_COOKIE).orEmpty()
        assertTrue(setCookie.startsWith("${SignupTokenCookieManager.COOKIE_NAME}=signup-token"))
        assertTrue(setCookie.contains("Path=/api"))
        assertTrue(setCookie.contains("Max-Age=600"))
        assertTrue(setCookie.contains("Secure"))
        assertTrue(setCookie.contains("HttpOnly"))
        assertTrue(setCookie.contains("SameSite=Lax"))
    }

    @Test
    fun `Signup Token 쿠키를 만료시켜 삭제한다`() {
        val response = MockHttpServletResponse()

        manager.clear(response)

        val setCookie = response.getHeader(HttpHeaders.SET_COOKIE).orEmpty()
        assertTrue(setCookie.startsWith("${SignupTokenCookieManager.COOKIE_NAME}="))
        assertTrue(setCookie.contains("Max-Age=0"))
        assertTrue(setCookie.contains("Path=/api"))
    }

    @Test
    fun `요청 쿠키에서 Signup Token을 찾는다`() {
        val request =
            MockHttpServletRequest().apply {
                setCookies(Cookie("other", "value"), Cookie(SignupTokenCookieManager.COOKIE_NAME, "signup-token"))
            }

        assertEquals("signup-token", manager.resolve(request))
    }
}
