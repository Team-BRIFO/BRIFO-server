package com.brifo.server.batch.settlement

import com.brifo.server.stock.entity.PendingUserStock
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.entity.UserStock
import com.brifo.server.stock.repository.PendingUserStockRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import kotlin.test.assertEquals

class PendingUserStockApplicationServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val userStockRepository = mock(UserStockRepository::class.java)
    private val pendingUserStockRepository = mock(PendingUserStockRepository::class.java)
    private val service =
        PendingUserStockApplicationService(userRepository, userStockRepository, pendingUserStockRepository)

    @Test
    fun `적용 시각이 지난 사용자의 관심 종목을 전체 교체한다`() {
        val effectiveAt = LocalDateTime.of(2026, 8, 3, 15, 50)
        val user = mock(User::class.java)
        val firstStock = mock(Stock::class.java)
        val secondStock = mock(Stock::class.java)
        val firstPending = mock(PendingUserStock::class.java)
        val secondPending = mock(PendingUserStock::class.java)
        `when`(userRepository.findForUpdateById(1L)).thenReturn(user)
        `when`(firstPending.stock).thenReturn(firstStock)
        `when`(secondPending.stock).thenReturn(secondStock)
        `when`(pendingUserStockRepository.findAllByUserAndEffectiveAtLessThanEqual(user, effectiveAt))
            .thenReturn(listOf(firstPending, secondPending))

        service.applyUser(1L, effectiveAt)

        verify(userStockRepository).deleteAllByUser(user)
        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<UserStock>>
        verify(userStockRepository).saveAll(captor.capture())
        assertEquals(listOf(firstStock, secondStock), captor.value.map { it.stock })
        verify(pendingUserStockRepository).deleteAllByUserAndEffectiveAtLessThanEqual(user, effectiveAt)
    }

    @Test
    fun `사용자 잠금 후 적용할 예약이 없으면 변경하지 않는다`() {
        val effectiveAt = LocalDateTime.of(2026, 8, 3, 15, 50)
        val user = mock(User::class.java)
        `when`(userRepository.findForUpdateById(1L)).thenReturn(user)
        `when`(pendingUserStockRepository.findAllByUserAndEffectiveAtLessThanEqual(user, effectiveAt))
            .thenReturn(emptyList())

        service.applyUser(1L, effectiveAt)

        verifyNoInteractions(userStockRepository)
    }
}
