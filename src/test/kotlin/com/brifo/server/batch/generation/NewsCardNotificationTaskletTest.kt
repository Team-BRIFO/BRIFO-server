package com.brifo.server.batch.generation

import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.notification.entity.NotificationCode
import com.brifo.server.notification.entity.NotificationTargetType
import com.brifo.server.notification.repository.NewsCardArrival
import com.brifo.server.notification.repository.NotificationRepository
import com.brifo.server.notification.service.NotificationCreationService
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.entity.UserStock
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.user.entity.User
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDate
import java.util.UUID

class NewsCardNotificationTaskletTest {
    private val cardRepository = mock(NewsCardRepository::class.java)
    private val userStockRepository = mock(UserStockRepository::class.java)
    private val notificationRepository = mock(NotificationRepository::class.java)
    private val notificationService = mock(NotificationCreationService::class.java)
    private val displayDate = LocalDate.of(2026, 8, 4)

    @Test
    fun `같은 표시일 사용자 종목 알림이 이미 있으면 다시 생성하지 않는다`() {
        val stock = mock(Stock::class.java)
        val userStock = mock(UserStock::class.java)
        val user = mock(User::class.java)
        val stockPublicId = UUID.randomUUID()
        `when`(stock.publicId).thenReturn(stockPublicId)
        `when`(user.id).thenReturn(2L)
        `when`(user.publicId).thenReturn(UUID.randomUUID())
        `when`(userStock.stock).thenReturn(stock)
        `when`(userStock.user).thenReturn(user)
        `when`(cardRepository.findDistinctStockIdsByDisplayDate(displayDate)).thenReturn(listOf(1L))
        `when`(userStockRepository.findAllByStockIdIn(listOf(1L), firstPage())).thenReturn(PageImpl(listOf(userStock)))
        `when`(
            notificationRepository.findNewsCardArrivals(
                setOf(2L),
                NotificationCode.NEWS_CARD_ARRIVED.name,
                NotificationTargetType.NEWS_CARD_LIST,
                setOf(stockPublicId),
                displayDate,
            ),
        ).thenReturn(setOf(NewsCardArrival(2L, stockPublicId)))

        tasklet().execute(mock(StepContribution::class.java), mock(ChunkContext::class.java))

        verifyNoInteractions(notificationService)
    }

    @Test
    fun `알림 생성에는 실행일이 아니라 카드 표시일을 전달한다`() {
        val stock = mock(Stock::class.java)
        val userStock = mock(UserStock::class.java)
        val user = mock(User::class.java)
        val userPublicId = UUID.randomUUID()
        val stockPublicId = UUID.randomUUID()
        `when`(stock.publicId).thenReturn(stockPublicId)
        `when`(user.id).thenReturn(2L)
        `when`(user.publicId).thenReturn(userPublicId)
        `when`(userStock.stock).thenReturn(stock)
        `when`(userStock.user).thenReturn(user)
        `when`(cardRepository.findDistinctStockIdsByDisplayDate(displayDate)).thenReturn(listOf(1L))
        `when`(userStockRepository.findAllByStockIdIn(listOf(1L), firstPage())).thenReturn(PageImpl(listOf(userStock)))
        `when`(
            notificationRepository.findNewsCardArrivals(
                setOf(2L),
                NotificationCode.NEWS_CARD_ARRIVED.name,
                NotificationTargetType.NEWS_CARD_LIST,
                setOf(stockPublicId),
                displayDate,
            ),
        ).thenReturn(emptySet())

        tasklet().execute(mock(StepContribution::class.java), mock(ChunkContext::class.java))

        verify(notificationService).create(
            userId = userPublicId,
            code = NotificationCode.NEWS_CARD_ARRIVED,
            target = NotificationCreationService.Target(NotificationTargetType.NEWS_CARD_LIST, stockPublicId),
            eventDate = displayDate,
        )
    }

    private fun tasklet() =
        NewsCardNotificationTasklet(
            displayDate,
            cardRepository,
            userStockRepository,
            notificationRepository,
            notificationService,
        )

    private fun firstPage() = PageRequest.of(0, 500, Sort.by("id"))
}
