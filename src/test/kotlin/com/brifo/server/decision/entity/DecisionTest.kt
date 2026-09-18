package com.brifo.server.decision.entity

import com.brifo.server.briefing.entity.Briefing
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import kotlin.test.assertFailsWith

class DecisionTest {
    @Test
    fun `배분 금액은 양수여야 한다`() {
        val briefing = mock(Briefing::class.java)

        Decision.create(briefing, DecisionDirection.UP, allocatedAp = 1000, allocationRatePercent = 10)

        assertFailsWith<IllegalArgumentException> {
            Decision.create(briefing, DecisionDirection.UP, allocatedAp = 0, allocationRatePercent = 10)
        }
    }

    @Test
    fun `배분 비율은 1부터 40까지만 허용한다`() {
        val briefing = mock(Briefing::class.java)

        listOf(1, 40).forEach { allocationRatePercent ->
            Decision.create(briefing, DecisionDirection.UP, allocatedAp = 1000, allocationRatePercent = allocationRatePercent)
        }
        listOf(0, 41).forEach { allocationRatePercent ->
            assertFailsWith<IllegalArgumentException> {
                Decision.create(briefing, DecisionDirection.UP, allocatedAp = 1000, allocationRatePercent = allocationRatePercent)
            }
        }
    }
}
