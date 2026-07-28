package com.brifo.server.badge.service

import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.service.ApTransactionService
import com.brifo.server.badge.code.BadgeCode
import com.brifo.server.badge.entity.Badge
import com.brifo.server.badge.exception.BadgeNotFoundException
import com.brifo.server.badge.repository.BadgeRepository
import com.brifo.server.badge.repository.UserBadgeRepository
import com.brifo.server.notification.entity.NotificationCode
import com.brifo.server.notification.entity.NotificationTargetType
import com.brifo.server.notification.service.NotificationCreationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.util.UUID

class BadgeAwardServiceTest {
    private lateinit var badgeRepository: BadgeRepository
    private lateinit var userBadgeRepository: UserBadgeRepository
    private lateinit var apTransactionService: ApTransactionService
    private lateinit var notificationCreationService: NotificationCreationService
    private lateinit var badgeAwardService: BadgeAwardService

    @BeforeEach
    fun setUp() {
        badgeRepository = mock(BadgeRepository::class.java)
        userBadgeRepository = mock(UserBadgeRepository::class.java)
        apTransactionService = mock(ApTransactionService::class.java)
        notificationCreationService = mock(NotificationCreationService::class.java)
        badgeAwardService =
            BadgeAwardService(
                badgeRepository,
                userBadgeRepository,
                apTransactionService,
                notificationCreationService,
            )
    }

    @Test
    fun `첫 획득은 뱃지 정보를 반환하고 awarded가 true이다`() {
        val userId = UUID.randomUUID()
        val badgeId = UUID.randomUUID()
        val badge = badge(1L, badgeId, 50)
        `when`(badgeRepository.findByCode("B01")).thenReturn(badge)
        `when`(userBadgeRepository.insertIfAbsent(userId, 1L)).thenReturn(1)
        `when`(userBadgeRepository.findIdByUserPublicIdAndBadgeId(userId, 1L)).thenReturn(10L)

        val result = badgeAwardService.awardBadge(userId, BadgeCode.B01)

        assertEquals(badgeId, result.badgeId)
        assertEquals(50, result.rewardAp)
        assertTrue(result.awarded)
        verify(userBadgeRepository).insertIfAbsent(userId, 1L)
        verify(apTransactionService).change(
            userId = userId,
            deltaAp = 50,
            reason = ApTransactionReason.BADGE,
            target = ApTransactionService.Target(ApTransactionTargetType.USER_BADGE, 10L),
        )
        verify(notificationCreationService).create(
            userId = userId,
            code = NotificationCode.BADGE_AWARDED,
            target = NotificationCreationService.Target(NotificationTargetType.BADGE, badgeId),
            eventId = 10L,
        )
    }

    @Test
    fun `이미 획득한 뱃지도 예외 없이 멱등 성공한다`() {
        val userId = UUID.randomUUID()
        val badgeId = UUID.randomUUID()
        val badge = badge(1L, badgeId, 50)
        `when`(badgeRepository.findByCode("B01")).thenReturn(badge)
        `when`(userBadgeRepository.insertIfAbsent(userId, 1L)).thenReturn(0)

        val result = badgeAwardService.awardBadge(userId, BadgeCode.B01)

        assertEquals(badgeId, result.badgeId)
        assertEquals(50, result.rewardAp)
        assertFalse(result.awarded)
        verifyNoInteractions(apTransactionService)
        verifyNoInteractions(notificationCreationService)
    }

    @Test
    fun `정의되지 않은 뱃지 코드는 BADGE_404 예외를 던진다`() {
        val userId = UUID.randomUUID()
        `when`(badgeRepository.findByCode("B01")).thenReturn(null)

        assertThrows(BadgeNotFoundException::class.java) {
            badgeAwardService.awardBadge(userId, BadgeCode.B01)
        }
    }

    private fun badge(
        id: Long,
        publicId: UUID,
        rewardAp: Int,
    ): Badge =
        mock(Badge::class.java).also {
            `when`(it.id).thenReturn(id)
            `when`(it.publicId).thenReturn(publicId)
            `when`(it.rewardAp).thenReturn(rewardAp)
        }
}
