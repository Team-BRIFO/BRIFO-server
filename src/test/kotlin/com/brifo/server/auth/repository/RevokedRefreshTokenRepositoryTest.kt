package com.brifo.server.auth.repository

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.auth.entity.RevokedRefreshToken
import com.brifo.server.global.config.JpaConfig
import com.brifo.server.global.config.QueryDslConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.UUID

@DataJpaTest
@Import(TestcontainersConfiguration::class, JpaConfig::class, QueryDslConfig::class)
@ActiveProfiles("test")
class RevokedRefreshTokenRepositoryTest {
    @Autowired
    private lateinit var repository: RevokedRefreshTokenRepository

    @Test
    fun `만료된 폐기 기록만 삭제한다`() {
        val now = Instant.parse("2026-07-28T19:00:00Z")
        val expiredTokenId = UUID.randomUUID().toString()
        val activeTokenId = UUID.randomUUID().toString()
        repository.saveAllAndFlush(
            listOf(
                revokedToken(expiredTokenId, now),
                revokedToken(activeTokenId, now.plusSeconds(1)),
            ),
        )

        val deletedCount = repository.deleteExpired(now)
        repository.flush()

        assertEquals(1, deletedCount)
        assertFalse(repository.existsByTokenId(expiredTokenId))
        assertTrue(repository.existsByTokenId(activeTokenId))
    }

    private fun revokedToken(
        tokenId: String,
        expiresAt: Instant,
    ): RevokedRefreshToken =
        RevokedRefreshToken.create(
            tokenId = tokenId,
            userPublicId = UUID.randomUUID(),
            expiresAt = expiresAt,
        )
}
