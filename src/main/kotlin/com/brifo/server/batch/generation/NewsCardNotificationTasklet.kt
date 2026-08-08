package com.brifo.server.batch.generation

import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.notification.entity.NotificationCode
import com.brifo.server.notification.entity.NotificationTargetType
import com.brifo.server.notification.repository.NewsCardArrival
import com.brifo.server.notification.repository.NotificationRepository
import com.brifo.server.notification.service.NotificationCreationService
import com.brifo.server.stock.repository.UserStockRepository
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDate

class NewsCardNotificationTasklet(
    private val displayDate: LocalDate,
    private val newsCardRepository: NewsCardRepository,
    private val userStockRepository: UserStockRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationCreationService: NotificationCreationService,
) : Tasklet {
    private var pageNumber = 0

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val stockIds = newsCardRepository.findDistinctStockIdsByDisplayDate(displayDate)
        if (stockIds.isEmpty()) return RepeatStatus.FINISHED

        val page =
            userStockRepository.findAllByStockIdIn(
                stockIds,
                PageRequest.of(pageNumber, PAGE_SIZE, Sort.by("id")),
            )
        if (page.isEmpty) return RepeatStatus.FINISHED

        val existingArrivals =
            notificationRepository.findNewsCardArrivals(
                userIds = page.map { requireNotNull(it.user.id) }.toSet(),
                code = NotificationCode.NEWS_CARD_ARRIVED.name,
                targetType = NotificationTargetType.NEWS_CARD_LIST,
                targetPublicIds = page.map { requireNotNull(it.stock.publicId) }.toSet(),
                eventDate = displayDate,
            )

        page.forEach { userStock ->
            val userId = requireNotNull(userStock.user.id)
            val userPublicId = requireNotNull(userStock.user.publicId)
            val stockPublicId = requireNotNull(userStock.stock.publicId)
            if (NewsCardArrival(userId, stockPublicId) !in existingArrivals) {
                notificationCreationService.create(
                    userId = userPublicId,
                    code = NotificationCode.NEWS_CARD_ARRIVED,
                    target = NotificationCreationService.Target(NotificationTargetType.NEWS_CARD_LIST, stockPublicId),
                    eventDate = displayDate,
                )
            }
        }
        pageNumber++
        return if (page.hasNext()) RepeatStatus.CONTINUABLE else RepeatStatus.FINISHED
    }

    private companion object {
        const val PAGE_SIZE = 500
    }
}
