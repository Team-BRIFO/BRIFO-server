package com.brifo.server.badge.service

import com.brifo.server.badge.exception.BadgeNotFoundException
import com.brifo.server.badge.repository.BadgeRepository
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class BadgeServiceTest {
    private lateinit var badgeRepository: BadgeRepository
    private lateinit var badgeService: BadgeService

    @BeforeEach
    fun setUp() {
        badgeRepository = mock(BadgeRepository::class.java)
        badgeService = BadgeService(badgeRepository)
    }

    @Test
    fun `뱃지가 없거나 보유하지 않으면 BADGE_404 예외를 던진다`() {
        val userId = UUID.randomUUID()
        val badgeId = UUID.randomUUID()
        `when`(badgeRepository.findOwnedBadge(userId, badgeId)).thenReturn(null)

        assertThrows(BadgeNotFoundException::class.java) {
            badgeService.getOwnedBadge(userId, badgeId)
        }
    }
}
