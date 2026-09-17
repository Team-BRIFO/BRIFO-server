package com.brifo.server.payment.exception

import com.brifo.server.global.exception.BusinessException
import com.brifo.server.payment.code.PaymentErrorCode

sealed class PaymentException(
    errorCode: PaymentErrorCode,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)

class InvalidChargeAmountException(
    message: String = PaymentErrorCode.INVALID_CHARGE_AMOUNT.message,
) : PaymentException(PaymentErrorCode.INVALID_CHARGE_AMOUNT, message)

class PaymentNotFoundException(
    message: String = PaymentErrorCode.PAYMENT_NOT_FOUND.message,
) : PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND, message)

class PaymentAlreadyProcessedException(
    message: String = PaymentErrorCode.PAYMENT_ALREADY_PROCESSED.message,
) : PaymentException(PaymentErrorCode.PAYMENT_ALREADY_PROCESSED, message)

class PaymentAmountMismatchException(
    message: String = PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH.message,
) : PaymentException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH, message)

class PaymentConfirmationFailedException(
    message: String = PaymentErrorCode.PAYMENT_CONFIRMATION_FAILED.message,
) : PaymentException(PaymentErrorCode.PAYMENT_CONFIRMATION_FAILED, message)
