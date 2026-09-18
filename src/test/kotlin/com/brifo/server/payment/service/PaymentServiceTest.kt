package com.brifo.server.payment.service

import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.service.ApTransactionService
import com.brifo.server.badge.service.BadgeAwardService
import com.brifo.server.payment.client.TossPaymentsClient
import com.brifo.server.payment.config.TossPaymentsProperties
import com.brifo.server.payment.dto.request.ConfirmPaymentRequest
import com.brifo.server.payment.dto.request.ReadyPaymentRequest
import com.brifo.server.payment.entity.Payment
import com.brifo.server.payment.exception.InvalidChargeAmountException
import com.brifo.server.payment.exception.PaymentAlreadyProcessedException
import com.brifo.server.payment.exception.PaymentAmountMismatchException
import com.brifo.server.payment.exception.PaymentConfirmationFailedException
import com.brifo.server.payment.exception.PaymentNotFoundException
import com.brifo.server.payment.repository.PaymentRepository
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class PaymentServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var paymentRepository: PaymentRepository
    private lateinit var tossPaymentsClient: TossPaymentsClient
    private lateinit var apTransactionService: ApTransactionService
    private lateinit var badgeAwardService: BadgeAwardService
    private lateinit var service: PaymentService
    private val properties =
        TossPaymentsProperties(
            clientKey = "test_ck_dummy",
            secretKey = "test_sk_dummy",
            minChargeAmount = 1_000,
            maxChargeAmount = 1_000_000,
        )
    private val clock = Clock.fixed(Instant.parse("2026-09-17T00:00:00Z"), ZoneOffset.UTC)
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        paymentRepository = mock(PaymentRepository::class.java)
        tossPaymentsClient = mock(TossPaymentsClient::class.java)
        apTransactionService = mock(ApTransactionService::class.java)
        badgeAwardService = mock(BadgeAwardService::class.java)
        service =
            PaymentService(
                userRepository = userRepository,
                paymentRepository = paymentRepository,
                tossPaymentsClient = tossPaymentsClient,
                apTransactionService = apTransactionService,
                badgeAwardService = badgeAwardService,
                properties = properties,
                clock = clock,
            )
    }

    @Test
    fun `충전 준비는 결제를 저장하고 클라이언트 키를 함께 내려준다`() {
        val user = user(userId)
        `when`(userRepository.findByPublicId(userId)).thenReturn(user)

        val response = service.ready(userId, ReadyPaymentRequest(50_000))

        assertEquals(50_000, response.amount)
        assertEquals("test_ck_dummy", response.clientKey)
        verify(paymentRepository).save(org.mockito.ArgumentMatchers.any(Payment::class.java))
    }

    @Test
    fun `충전 금액이 허용 범위를 벗어나면 예외를 던진다`() {
        assertThrows<InvalidChargeAmountException> {
            service.ready(userId, ReadyPaymentRequest(100))
        }
        assertThrows<InvalidChargeAmountException> {
            service.ready(userId, ReadyPaymentRequest(10_000_000))
        }
    }

    @Test
    fun `결제 승인에 성공하면 결제를 완료 처리하고 포인트를 충전한다`() {
        val user = user(userId)
        val payment = Payment.create(user, "charge-1", 50_000)
        `when`(paymentRepository.findByOrderId("charge-1")).thenReturn(payment)
        `when`(tossPaymentsClient.confirm("pay-key", "charge-1", 50_000)).thenReturn(
            Result.success(
                TossPaymentsClient.ConfirmedPayment(
                    paymentKey = "pay-key",
                    orderId = "charge-1",
                    totalAmount = 50_000,
                    method = "카드",
                    approvedAt = "2026-09-17T00:00:00+09:00",
                ),
            ),
        )
        `when`(apTransactionService.change(userId, 50_000, ApTransactionReason.CHARGE)).thenReturn(150_000)

        val response = service.confirm(userId, ConfirmPaymentRequest("pay-key", "charge-1", 50_000))

        assertEquals(150_000, response.balanceAp)
        verify(apTransactionService).change(userId, 50_000, ApTransactionReason.CHARGE)
        verify(badgeAwardService).awardBadge(userId, com.brifo.server.badge.code.BadgeCode.B40)
        verify(badgeAwardService).awardBalanceMilestones(userId, 150_000)
    }

    @Test
    fun `존재하지 않는 결제는 예외를 던진다`() {
        `when`(paymentRepository.findByOrderId("unknown")).thenReturn(null)

        assertThrows<PaymentNotFoundException> {
            service.confirm(userId, ConfirmPaymentRequest("pay-key", "unknown", 50_000))
        }
    }

    @Test
    fun `이미 처리된 결제를 다시 승인하면 예외를 던진다`() {
        val user = user(userId)
        val payment = Payment.create(user, "charge-1", 50_000)
        payment.markDone("pay-key", "카드", java.time.LocalDateTime.now())
        `when`(paymentRepository.findByOrderId("charge-1")).thenReturn(payment)

        assertThrows<PaymentAlreadyProcessedException> {
            service.confirm(userId, ConfirmPaymentRequest("pay-key", "charge-1", 50_000))
        }
    }

    @Test
    fun `승인 요청 금액이 결제 금액과 다르면 예외를 던진다`() {
        val user = user(userId)
        val payment = Payment.create(user, "charge-1", 50_000)
        `when`(paymentRepository.findByOrderId("charge-1")).thenReturn(payment)

        assertThrows<PaymentAmountMismatchException> {
            service.confirm(userId, ConfirmPaymentRequest("pay-key", "charge-1", 99_999))
        }
    }

    @Test
    fun `토스 승인이 거절되면 결제를 실패 처리하고 예외를 던진다`() {
        val user = user(userId)
        val payment = Payment.create(user, "charge-1", 50_000)
        `when`(paymentRepository.findByOrderId("charge-1")).thenReturn(payment)
        `when`(tossPaymentsClient.confirm("pay-key", "charge-1", 50_000)).thenReturn(
            Result.failure(TossPaymentsClient.TossPaymentDeclinedException("카드 승인이 거절됐습니다.")),
        )

        assertThrows<PaymentConfirmationFailedException> {
            service.confirm(userId, ConfirmPaymentRequest("pay-key", "charge-1", 50_000))
        }

        assertEquals(com.brifo.server.payment.entity.PaymentStatus.FAILED, payment.status)
        org.mockito.Mockito.verifyNoInteractions(apTransactionService)
    }

    private fun user(publicId: UUID): User =
        mock(User::class.java).also {
            `when`(it.publicId).thenReturn(publicId)
        }
}
