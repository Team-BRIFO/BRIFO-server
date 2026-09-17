package com.brifo.server.payment.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class PaymentErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    INVALID_CHARGE_AMOUNT(
        HttpStatus.BAD_REQUEST,
        "PAYMENT_400_01",
        "충전 금액이 올바르지 않습니다.",
    ),
    PAYMENT_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "PAYMENT_404",
        "결제 정보를 찾을 수 없습니다.",
    ),
    PAYMENT_ALREADY_PROCESSED(
        HttpStatus.CONFLICT,
        "PAYMENT_409_01",
        "이미 처리된 결제입니다.",
    ),
    PAYMENT_AMOUNT_MISMATCH(
        HttpStatus.CONFLICT,
        "PAYMENT_409_02",
        "결제 금액이 일치하지 않습니다.",
    ),
    PAYMENT_CONFIRMATION_FAILED(
        HttpStatus.BAD_GATEWAY,
        "PAYMENT_502",
        "결제 승인에 실패했습니다.",
    ),
}
