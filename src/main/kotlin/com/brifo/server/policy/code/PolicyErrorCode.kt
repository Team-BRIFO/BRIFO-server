package com.brifo.server.policy.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class PolicyErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    REQUIRED_POLICY_MISSING(HttpStatus.BAD_REQUEST, "POLICY_400_01", "필수 약관 동의가 누락되었습니다."),
    REQUIRED_POLICY_CANNOT_BE_REVOKED(HttpStatus.BAD_REQUEST, "POLICY_400_02", "필수 약관은 철회할 수 없습니다."),
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "POLICY_404", "약관을 찾을 수 없습니다."),
}
