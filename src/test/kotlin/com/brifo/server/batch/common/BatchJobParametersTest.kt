package com.brifo.server.batch.common

import com.brifo.server.batch.collection.CollectionRound
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class BatchJobParametersTest {
    @Test
    fun `재시작 식별값에는 대상 날짜와 수집 회차만 포함한다`() {
        val parameters = BatchJobParameters.forCollection(LocalDate.of(2026, 8, 3), CollectionRound.MIDDAY.name)

        assertEquals("2026-08-03", parameters.getString(BatchJobParameters.TARGET_DATE))
        assertEquals("MIDDAY", parameters.getString(BatchJobParameters.COLLECTION_ROUND))
        assertNull(parameters.getString("currentTime"))
        assertNull(parameters.getString("attempt"))
    }

    @Test
    fun `개발 수동 실행은 매번 새로운 실행 식별값을 사용한다`() {
        val targetDate = LocalDate.of(2026, 8, 3)

        val first = BatchJobParameters.forDevDate(targetDate)
        val second = BatchJobParameters.forDevDate(targetDate)

        assertEquals("2026-08-03", first.getString(BatchJobParameters.TARGET_DATE))
        assertEquals("true", first.getString(BatchJobParameters.IGNORE_SETTLEMENT_CUTOFF))
        assertNotEquals(first.getString(BatchJobParameters.DEV_RUN_ID), second.getString(BatchJobParameters.DEV_RUN_ID))
    }
}
