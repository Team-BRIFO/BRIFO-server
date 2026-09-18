package com.brifo.server.payment.service

import com.brifo.server.ap.dto.response.ApBalanceResponse
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.service.ApTransactionService
import com.brifo.server.badge.code.BadgeCode
import com.brifo.server.badge.service.BadgeAwardService
import com.brifo.server.payment.client.TossPaymentsClient
import com.brifo.server.payment.config.TossPaymentsProperties
import com.brifo.server.payment.dto.request.ConfirmPaymentRequest
import com.brifo.server.payment.dto.request.ReadyPaymentRequest
import com.brifo.server.payment.dto.response.ReadyPaymentResponse
import com.brifo.server.payment.entity.Payment
import com.brifo.server.payment.entity.PaymentStatus
import com.brifo.server.payment.exception.InvalidChargeAmountException
import com.brifo.server.payment.exception.PaymentAlreadyProcessedException
import com.brifo.server.payment.exception.PaymentAmountMismatchException
import com.brifo.server.payment.exception.PaymentConfirmationFailedException
import com.brifo.server.payment.exception.PaymentNotFoundException
import com.brifo.server.payment.repository.PaymentRepository
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class PaymentService(
    private val userRepository: UserRepository,
    private val paymentRepository: PaymentRepository,
    private val tossPaymentsClient: TossPaymentsClient,
    private val apTransactionService: ApTransactionService,
    private val badgeAwardService: BadgeAwardService,
    private val properties: TossPaymentsProperties,
    private val clock: Clock,
) {
    @Transactional
    fun ready(
        userId: UUID,
        request: ReadyPaymentRequest,
    ): ReadyPaymentResponse {
        if (request.amount !in properties.minChargeAmount..properties.maxChargeAmount) {
            throw InvalidChargeAmountException(
                "충전 금액은 ${properties.minChargeAmount}원 이상 ${properties.maxChargeAmount}원 이하여야 합니다.",
            )
        }
        val user = userRepository.findByPublicId(userId) ?: throw UserNotFoundException()
        // 토스 orderId는 영수증·대사에 쓰이는 대외 식별자라 publicId 조합이 아니라 별도로 발급한다.
        val orderId = "charge-${UUID.randomUUID()}"
        paymentRepository.save(Payment.create(user, orderId, request.amount))
        return ReadyPaymentResponse(orderId, request.amount, properties.clientKey)
    }

    @Transactional
    fun confirm(
        userId: UUID,
        request: ConfirmPaymentRequest,
    ): ApBalanceResponse {
        val payment = paymentRepository.findByOrderId(request.orderId) ?: throw PaymentNotFoundException()
        check(payment.user.publicId == userId) { "본인의 결제만 승인할 수 있습니다." }
        if (payment.status != PaymentStatus.READY) throw PaymentAlreadyProcessedException()
        if (payment.amount != request.amount) throw PaymentAmountMismatchException()

        val result = tossPaymentsClient.confirm(request.paymentKey, request.orderId, request.amount)
        val confirmed =
            result.getOrElse { error ->
                payment.markFailed(error.message ?: "결제 승인에 실패했습니다.")
                throw PaymentConfirmationFailedException(error.message ?: PaymentConfirmationFailedException().message)
            }

        payment.markDone(confirmed.paymentKey, confirmed.method, LocalDateTime.now(clock))
        val balanceAp =
            apTransactionService.change(
                userId = userId,
                deltaAp = payment.amount * CHARGE_EXCHANGE_MULTIPLIER,
                reason = ApTransactionReason.CHARGE,
            )
        badgeAwardService.awardBadge(userId, BadgeCode.B40)
        badgeAwardService.awardBalanceMilestones(userId, balanceAp)
        return ApBalanceResponse(balanceAp)
    }

    private companion object {
        /** 결제 1원당 지급하는 게임 재화 배율. 실제 결제 금액과 게임 내 자금을 분리해 소액 결제로도 충분히 놀 수 있게 한다. */
        const val CHARGE_EXCHANGE_MULTIPLIER = 50
    }
}
