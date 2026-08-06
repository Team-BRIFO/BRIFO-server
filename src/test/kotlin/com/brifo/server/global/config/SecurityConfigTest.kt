package com.brifo.server.global.config

import com.brifo.server.auth.security.SignupTokenCookieManager
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.assertEquals

class SecurityConfigTest {
    @Test
    fun `CORS 응답에서 Signup CSRF 헤더를 노출한다`() {
        val source = SecurityConfig(CorsProperties(listOf("https://front.example.com"))).corsConfigurationSource()
        val request = MockHttpServletRequest("POST", "/api/auth/login/kakao")

        val configuration = requireNotNull(source.getCorsConfiguration(request))

        assertEquals(
            listOf(SignupTokenCookieManager.CSRF_HEADER_NAME),
            configuration.exposedHeaders,
        )
    }
}
