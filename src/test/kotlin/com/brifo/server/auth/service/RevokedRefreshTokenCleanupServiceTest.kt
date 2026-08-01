package com.brifo.server.auth.service

import com.brifo.server.auth.repository.RevokedRefreshTokenRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
class RevokedRefreshTokenCleanupServiceTest {
    @Mock
    private lateinit var revokedRefreshTokenRepository: RevokedRefreshTokenRepository

    @Test
    fun `만료 시각이 지난 Refresh Token 폐기 기록을 삭제한다`() {
        val now = Instant.parse("2026-07-28T19:00:00Z")
        val clock = Clock.fixed(now, ZoneOffset.UTC)
        val service = RevokedRefreshTokenCleanupService(revokedRefreshTokenRepository, clock)

        service.cleanup()

        verify(revokedRefreshTokenRepository).deleteExpired(now)
    }
}
