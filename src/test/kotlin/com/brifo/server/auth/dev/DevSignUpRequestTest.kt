package com.brifo.server.auth.dev

import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DevSignUpRequestTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `개발 회원가입 비밀번호는 공백일 수 없다`() {
        val violations = validator.validate(DevSignUpRequest(" "))

        assertTrue(violations.any { it.propertyPath.toString() == "password" })
    }

    @Test
    fun `개발 회원가입 초기 AP는 1 이상 500 이하여야 한다`() {
        listOf(0, 501).forEach { initialBalanceAp ->
            val violations = validator.validate(DevSignUpRequest("password", initialBalanceAp))

            assertTrue(violations.any { it.propertyPath.toString() == "initialBalanceAp" })
        }
    }

    @Test
    fun `개발 회원가입 초기 AP를 생략하거나 허용 범위로 지정할 수 있다`() {
        assertFalse(validator.validate(DevSignUpRequest("password")).any())
        assertFalse(validator.validate(DevSignUpRequest("password", 15)).any())
    }
}
