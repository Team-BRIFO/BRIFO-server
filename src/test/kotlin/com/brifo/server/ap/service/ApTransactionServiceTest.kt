package com.brifo.server.ap.service

import com.brifo.server.ap.entity.ApTransaction
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.exception.InsufficientApBalanceException
import com.brifo.server.ap.repository.ApTransactionRepository
import com.brifo.server.user.entity.User
import com.brifo.server.user.exception.UserNotFoundException
import com.brifo.server.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApTransactionServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var apTransactionRepository: ApTransactionRepository
    private lateinit var service: ApTransactionService

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        apTransactionRepository = mock(ApTransactionRepository::class.java)
        service = ApTransactionService(userRepository, apTransactionRepository)
    }

    @Test
    fun `양수 변경은 잠근 사용자 잔액과 원장을 함께 증가시킨다`() {
        val userId = UUID.randomUUID()
        val user = statefulUser(id = 1L, balance = 100)
        `when`(userRepository.findForUpdateByPublicId(userId)).thenReturn(user)

        val balance = service.change(userId, 50, ApTransactionReason.TUTORIAL)

        assertEquals(150, balance)
        val transaction = captureSavedTransaction()
        assertEquals(50, transaction.amount)
        assertEquals(ApTransactionReason.TUTORIAL, transaction.reason)
        assertEquals(null, transaction.targetType)
        assertEquals(null, transaction.targetId)
    }

    @Test
    fun `음수 변경은 잔액과 같은 경계 금액까지 허용하고 그대로 원장에 저장한다`() {
        val userId = UUID.randomUUID()
        val user = statefulUser(id = 1L, balance = 40)
        val target = ApTransactionService.Target(ApTransactionTargetType.BRIEFING, 31L)
        `when`(userRepository.findForUpdateByPublicId(userId)).thenReturn(user)

        val balance = service.change(userId, -40, ApTransactionReason.SALARY, target)

        assertEquals(0, balance)
        val transaction = captureSavedTransaction()
        assertEquals(-40, transaction.amount)
        assertEquals(ApTransactionReason.SALARY, transaction.reason)
        assertEquals(ApTransactionTargetType.BRIEFING, transaction.targetType)
        assertEquals(31L, transaction.targetId)
    }

    @Test
    fun `차감액이 잔액보다 1 크면 잔액과 원장을 변경하지 않는다`() {
        val userId = UUID.randomUUID()
        val user = statefulUser(id = 1L, balance = 40)
        `when`(userRepository.findForUpdateByPublicId(userId)).thenReturn(user)

        assertFailsWith<InsufficientApBalanceException> {
            service.change(
                userId,
                -41,
                ApTransactionReason.SALARY,
                ApTransactionService.Target(ApTransactionTargetType.BRIEFING, 31L),
            )
        }

        assertEquals(40, user.balanceAp)
        verify(apTransactionRepository, never()).save(any(ApTransaction::class.java))
    }

    @Test
    fun `존재하지 않는 사용자는 원장을 저장하지 않고 USER_404를 던진다`() {
        val userId = UUID.randomUUID()
        `when`(userRepository.findForUpdateByPublicId(userId)).thenReturn(null)

        assertFailsWith<UserNotFoundException> {
            service.change(userId, 50, ApTransactionReason.TUTORIAL)
        }
        verify(apTransactionRepository, never()).save(any(ApTransaction::class.java))
    }

    private fun statefulUser(
        id: Long,
        balance: Int,
    ): User {
        val state = AtomicInteger(balance)
        return mock(User::class.java).also { user ->
            `when`(user.id).thenReturn(id)
            `when`(user.balanceAp).thenAnswer { state.get() }
            doAnswer { invocation ->
                state.addAndGet(invocation.getArgument(0))
                null
            }.`when`(user).changeAp(anyInt())
        }
    }

    private fun captureSavedTransaction(): ApTransaction {
        val captor = ArgumentCaptor.forClass(ApTransaction::class.java)
        verify(apTransactionRepository).save(captor.capture())
        return captor.value
    }
}
