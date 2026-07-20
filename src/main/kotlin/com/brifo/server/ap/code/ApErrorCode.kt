package com.brifo.server.ap.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class ApErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    ATTENDANCE_REWARD_ALREADY_CLAIMED(
        HttpStatus.CONFLICT,
        "AP_409_01",
        "이미 오늘 출석 보상을 받았습니다.",
    ),
    TUTORIAL_REWARD_ALREADY_CLAIMED(
        HttpStatus.CONFLICT,
        "AP_409_02",
        "이미 튜토리얼 보상을 받았습니다.",
    ),
    CREDIT_LOAN_ALREADY_CLAIMED(
        HttpStatus.CONFLICT,
        "AP_409_03",
        "이미 신용대출을 사용했습니다.",
    ),
    CREDIT_LOAN_NOT_ELIGIBLE(
        HttpStatus.CONFLICT,
        "AP_409_04",
        "신용대출 조건을 충족하지 않습니다.",
    ),
    INSUFFICIENT_AP_BALANCE(
        HttpStatus.CONFLICT,
        "AP_409_05",
        "AP가 부족합니다.",
    ),
}
