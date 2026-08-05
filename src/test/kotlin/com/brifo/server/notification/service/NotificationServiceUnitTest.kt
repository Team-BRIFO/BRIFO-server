package com.brifo.server.notification.service

import com.brifo.server.notification.dto.request.GetNotificationsRequest
import com.brifo.server.notification.dto.response.GetNotificationsResponse
import com.brifo.server.notification.entity.NotificationTargetType
import com.brifo.server.notification.repository.NotificationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.UUID

class NotificationServiceUnitTest {
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var service: NotificationService

    @BeforeEach
    fun setUp() {
        notificationRepository = mock(NotificationRepository::class.java)
        service = NotificationService(notificationRepository)
    }

    @Test
    fun `빈 목록은 다음 페이지가 없다`() {
        val userId = UUID.randomUUID()
        `when`(notificationRepository.findPageByUserPublicId(userId, null, 11)).thenReturn(emptyList())

        val response = service.getNotifications(userId, GetNotificationsRequest())

        assertTrue(response.page.items.isEmpty())
        assertFalse(response.page.hasNext)
        assertNull(response.page.nextCursor)
    }

    @Test
    fun `size보다 하나 더 조회하면 size개와 마지막 항목 커서를 반환한다`() {
        val userId = UUID.randomUUID()
        val cursor = UUID.randomUUID()
        val items = (1..3).map(::item)
        `when`(notificationRepository.findPageByUserPublicId(userId, cursor, 3)).thenReturn(items)

        val response = service.getNotifications(userId, GetNotificationsRequest(cursor = cursor, size = 2))

        assertEquals(items.take(2), response.page.items)
        assertTrue(response.page.hasNext)
        assertEquals(items[1].notificationId, response.page.nextCursor)
        verify(notificationRepository).findPageByUserPublicId(userId, cursor, 3)
    }

    private fun item(index: Int): GetNotificationsResponse.NotificationItem =
        GetNotificationsResponse.NotificationItem(
            notificationId = UUID.fromString("00000000-0000-0000-0000-${index.toString().padStart(12, '0')}"),
            code = "DECISION_RESULT",
            title = "오늘의 정산이 끝났어요",
            body = "본문",
            createdAt = LocalDateTime.of(2026, 7, 23, 10, index),
            target = GetNotificationsResponse.Target(NotificationTargetType.DECISION, UUID.randomUUID()),
        )
}
