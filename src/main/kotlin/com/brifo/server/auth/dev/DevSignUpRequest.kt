package com.brifo.server.auth.dev

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

class DevSignUpRequest(
    @field:NotBlank
    val password: String,
    @field:Min(1)
    @field:Max(2_000_000)
    @field:Schema(
        description = "개발용 계정의 초기 AP 잔액입니다. 생략하면 기본 초기 지급액(1,000,000 AP)이 유지됩니다.",
        example = "15000",
        nullable = true,
    )
    val initialBalanceAp: Int? = null,
) {
    override fun toString(): String =
        "DevSignUpRequest(password=******, initialBalanceAp=$initialBalanceAp)"
}
