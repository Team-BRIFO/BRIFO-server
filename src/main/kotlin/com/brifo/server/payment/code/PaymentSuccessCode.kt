package com.brifo.server.payment.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class PaymentSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    PAYMENT_READY(
        HttpStatus.OK,
        "PAYMENT_200_01",
        "결제 준비가 완료되었습니다.",
    ),
    PAYMENT_CONFIRMED(
        HttpStatus.OK,
        "PAYMENT_200_02",
        "포인트 충전이 완료되었습니다.",
    ),
}
