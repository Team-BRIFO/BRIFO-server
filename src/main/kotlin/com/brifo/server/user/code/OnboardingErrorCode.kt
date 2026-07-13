package com.brifo.server.user.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class OnboardingErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    REQUIRED_POLICIES_NOT_AGREED(
        HttpStatus.CONFLICT,
        "ONBOARDING_409_01",
        "필수 약관 동의가 완료되지 않았습니다.",
    ),
    PROFILE_NOT_COMPLETED(
        HttpStatus.CONFLICT,
        "ONBOARDING_409_02",
        "프로필 입력이 완료되지 않았습니다.",
    ),
    STOCKS_NOT_SELECTED(
        HttpStatus.CONFLICT,
        "ONBOARDING_409_03",
        "관심 종목 선택이 완료되지 않았습니다.",
    ),
    ONBOARDING_ALREADY_COMPLETED(
        HttpStatus.CONFLICT,
        "ONBOARDING_409_04",
        "이미 온보딩이 완료되었습니다.",
    ),
}
