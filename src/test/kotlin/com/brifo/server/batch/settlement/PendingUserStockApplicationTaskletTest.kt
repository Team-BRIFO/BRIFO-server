package com.brifo.server.batch.settlement

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class PendingUserStockApplicationTaskletTest {
    private val service = mock(PendingUserStockApplicationService::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-03T06:50:00Z"), ZoneId.of("Asia/Seoul"))
    private val tasklet = PendingUserStockApplicationTasklet(service, clock)

    @Test
    fun `적용 대상 전체 사용자의 관심 종목을 반영한다`() {
        val effectiveAt = LocalDateTime.of(2026, 8, 3, 15, 50)
        `when`(service.findTargetUserIds(effectiveAt)).thenReturn(listOf(1L, 2L))

        tasklet.execute(mock(), mock())

        verify(service).applyUser(1L, effectiveAt)
        verify(service).applyUser(2L, effectiveAt)
    }
}
