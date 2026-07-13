package com.brifo.server.ap.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class ApSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    ATTENDANCE_REWARD_CLAIMED(
        HttpStatus.OK,
        "AP_200_01",
        "출석 보상을 받았습니다.",
    ),
    TUTORIAL_REWARD_CLAIMED(
        HttpStatus.OK,
        "AP_200_02",
        "튜토리얼 보상을 받았습니다.",
    ),
    CREDIT_LOAN_CLAIMED(
        HttpStatus.OK,
        "AP_200_03",
        "신용대출 AP를 받았습니다.",
    ),
}
