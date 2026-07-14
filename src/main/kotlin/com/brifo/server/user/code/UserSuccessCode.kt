package com.brifo.server.user.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class UserSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    ONBOARDING_PROFILE_UPDATED(
        HttpStatus.OK,
        "USER_200_01",
        "기본 정보가 저장되었습니다.",
    ),
    ONBOARDING_COMPLETED(
        HttpStatus.OK,
        "USER_200_02",
        "회원가입이 완료되었습니다.",
    ),
    PROFILE_UPDATED(
        HttpStatus.OK,
        "USER_200_03",
        "프로필이 수정되었습니다.",
    ),
    USER_DELETED(
        HttpStatus.OK,
        "USER_200_04",
        "회원 탈퇴가 완료되었습니다.",
    ),
}
