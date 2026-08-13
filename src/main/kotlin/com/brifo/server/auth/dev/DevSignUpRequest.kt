package com.brifo.server.auth.dev

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

class DevSignUpRequest(
    @field:NotBlank
    val password: String,
    @field:Min(1)
    @field:Max(500)
    @field:Schema(
        description = "개발용 계정의 초기 AP 잔액입니다. 생략하면 기본값 500 AP가 지급됩니다.",
        example = "15",
        nullable = true,
    )
    val initialBalanceAp: Int? = null,
) {
    override fun toString(): String =
        "DevSignUpRequest(password=******, initialBalanceAp=$initialBalanceAp)"
}
