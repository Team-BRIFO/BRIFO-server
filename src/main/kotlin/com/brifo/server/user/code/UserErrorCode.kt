package com.brifo.server.user.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class UserErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    INVALID_NICKNAME(
        HttpStatus.BAD_REQUEST,
        "USER_400_01",
        "유효하지 않은 닉네임입니다.",
    ),
    INVALID_COMPANY_NAME(
        HttpStatus.BAD_REQUEST,
        "USER_400_02",
        "유효하지 않은 회사명입니다.",
    ),
    USER_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "USER_404",
        "사용자를 찾을 수 없습니다.",
    ),
}
