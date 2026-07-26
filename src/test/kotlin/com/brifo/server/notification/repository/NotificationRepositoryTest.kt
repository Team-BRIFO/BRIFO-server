package com.brifo.server.notification.repository

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.global.config.JpaConfig
import com.brifo.server.global.config.QueryDslConfig
import com.brifo.server.notification.entity.Notification
import com.brifo.server.notification.entity.NotificationTargetType
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@Import(TestcontainersConfiguration::class, JpaConfig::class, QueryDslConfig::class)
@ActiveProfiles("test")
class NotificationRepositoryTest {
    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var notificationTypeRepository: NotificationTypeRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `커서 조회는 사용자 알림만 publicId 내림차순으로 제한한다`() {
        val user = saveUser("notification-user")
        val otherUser = saveUser("other-user")
        val type = requireNotNull(notificationTypeRepository.findByCode("NEWS_CARD_ARRIVED"))
        val userNotifications =
            (1..3).map { index ->
                notificationRepository
                    .saveAndFlush(
                        Notification.create(
                            user = user,
                            notificationType = type,
                            title = "새 카드뉴스 ${index}건이 도착했어요",
                            body = "새 소식이 올라왔어요",
                            targetType = NotificationTargetType.NEWS_CARD_LIST,
                        ),
                    ).also(entityManager::refresh)
            }
        notificationRepository.saveAndFlush(
            Notification.create(
                user = otherUser,
                notificationType = type,
                title = "다른 사용자 알림",
                body = null,
                targetType = NotificationTargetType.NONE,
            ),
        )
        val newestFirst = userNotifications.sortedByDescending { requireNotNull(it.publicId) }
        entityManager.clear()

        val firstPage =
            notificationRepository.findPageByUserPublicId(
                userPublicId = requireNotNull(user.publicId),
                cursor = null,
                limit = 2,
            )
        val afterCursor =
            notificationRepository.findPageByUserPublicId(
                userPublicId = requireNotNull(user.publicId),
                cursor = requireNotNull(newestFirst.first().publicId),
                limit = 10,
            )

        assertEquals(newestFirst.take(2).map { it.publicId }, firstPage.map { it.notificationId })
        assertEquals(newestFirst.drop(1).map { it.publicId }, afterCursor.map { it.notificationId })
        assertEquals(listOf("NEWS_CARD_ARRIVED", "NEWS_CARD_ARRIVED"), firstPage.map { it.code })
    }

    private fun saveUser(socialId: String): User =
        userRepository.saveAndFlush(
            User.create(
                provider = OAuthProvider.KAKAO,
                socialId = socialId,
                email = "$socialId@example.com",
            ),
        ).also(entityManager::refresh)
}
