package com.brifo.server.notification.entity

import com.brifo.server.user.entity.User
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.util.UUID
import kotlin.test.assertFailsWith

class NotificationTest {
    private val user = mock(User::class.java)
    private val type = mock(NotificationType::class.java)

    @Test
    fun `식별자가 필요한 대상은 공개 ID가 있어야 한다`() {
        val requiredTypes =
            listOf(
                NotificationTargetType.DECISION,
                NotificationTargetType.BRIEFING,
                NotificationTargetType.BADGE,
                NotificationTargetType.AGENT,
                NotificationTargetType.STOCK_BRIEFINGS,
                NotificationTargetType.NEWS_CARD_LIST,
            )

        requiredTypes.forEach { targetType ->
            assertFailsWith<IllegalArgumentException> {
                notification(targetType, null)
            }
            notification(targetType, UUID.randomUUID())
        }
    }

    @Test
    fun `목록과 이동 없는 대상은 공개 ID를 허용하지 않는다`() {
        listOf(NotificationTargetType.NONE).forEach { targetType ->
            notification(targetType, null)
            assertFailsWith<IllegalArgumentException> {
                notification(targetType, UUID.randomUUID())
            }
        }
    }

    private fun notification(
        targetType: NotificationTargetType,
        targetId: UUID?,
    ): Notification =
        Notification.create(
            user = user,
            notificationType = type,
            title = "제목",
            body = "본문",
            targetType = targetType,
            targetPublicId = targetId,
        )
}
