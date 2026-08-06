package com.brifo.server.auth.dev

import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class DevSignUpRequestTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `개발 회원가입 비밀번호는 공백일 수 없다`() {
        val violations = validator.validate(DevSignUpRequest(" "))

        assertTrue(violations.any { it.propertyPath.toString() == "password" })
    }
}
