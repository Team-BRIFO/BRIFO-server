package com.brifo.server.decision.entity

import com.brifo.server.briefing.entity.Briefing
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import kotlin.test.assertFailsWith

class DecisionTest {
    @Test
    fun `확신도는 1부터 5까지만 허용한다`() {
        val briefing = mock(Briefing::class.java)

        listOf(1, 5).forEach { confidenceLevel ->
            Decision.create(briefing, DecisionDirection.UP, confidenceLevel)
        }
        listOf(0, 6).forEach { confidenceLevel ->
            assertFailsWith<IllegalArgumentException> {
                Decision.create(briefing, DecisionDirection.UP, confidenceLevel)
            }
        }
    }
}
