package com.brifo.server.decision.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class DecisionErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    DECISION_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "DECISION_404",
        "예측을 찾을 수 없습니다.",
    ),
    DECISION_ALREADY_EXISTS(
        HttpStatus.CONFLICT,
        "DECISION_409_01",
        "이미 예측을 등록했습니다.",
    ),
}
