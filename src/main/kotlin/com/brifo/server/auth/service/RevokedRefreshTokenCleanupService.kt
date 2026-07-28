package com.brifo.server.auth.service

import com.brifo.server.auth.repository.RevokedRefreshTokenRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class RevokedRefreshTokenCleanupService(
    private val revokedRefreshTokenRepository: RevokedRefreshTokenRepository,
    private val clock: Clock,
) {
    @Scheduled(cron = "\${app.auth.refresh-token.cleanup-cron}", zone = "Asia/Seoul")
    @Transactional
    fun cleanup() {
        revokedRefreshTokenRepository.deleteExpired(Instant.now(clock))
    }
}
