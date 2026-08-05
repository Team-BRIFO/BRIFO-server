package com.brifo.server.auth.dto.request

import io.swagger.v3.core.converter.ModelConverters
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RefreshTokenRequestTest {
    @Test
    fun `OpenAPI 스키마에서 Refresh Token은 필수이다`() {
        val schema = ModelConverters.getInstance().read(RefreshTokenRequest::class.java).getValue("RefreshTokenRequest")

        assertEquals(listOf("refreshToken"), schema.required)
    }
}
