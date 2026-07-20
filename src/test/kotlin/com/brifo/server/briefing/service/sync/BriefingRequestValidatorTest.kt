package com.brifo.server.briefing.service.sync

import com.brifo.server.briefing.code.BriefingErrorCode
import com.brifo.server.briefing.exception.BriefingRequestClosedException
import com.brifo.server.briefing.service.sync.BriefingRequestTask
import com.brifo.server.briefing.service.sync.BriefingRequestValidator
import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.exception.BusinessException
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BriefingRequestValidatorTest {
    private val validator = BriefingRequestValidator()

    @Test
    fun `사원을 한 명 이상 세 명 이하로 선택하면 요청할 수 있다`() {
        validator.validate(command(agentPublicIds = List(3) { UUID.randomUUID() }))
    }

    @Test
    fun `사원을 선택하지 않으면 잘못된 요청이다`() {
        assertInvalidAgentSelection(emptyList())
    }

    @Test
    fun `사원을 네 명 이상 선택하면 잘못된 요청이다`() {
        assertInvalidAgentSelection(List(4) { UUID.randomUUID() })
    }

    @Test
    fun `같은 사원을 중복 선택하면 잘못된 요청이다`() {
        val agentPublicId = UUID.randomUUID()

        assertInvalidAgentSelection(listOf(agentPublicId, agentPublicId))
    }

    @Test
    fun `오후 세 시 이십 분부터 요청이 마감된다`() {
        val exception = assertFailsWith<BriefingRequestClosedException> {
            validator.validate(command(requestedAt = LocalDateTime.of(2026, 7, 18, 15, 20)))
        }

        assertEquals(BriefingErrorCode.BRIEFING_REQUEST_CLOSED, exception.errorCode)
    }

    private fun assertInvalidAgentSelection(agentPublicIds: List<UUID>) {
        val exception = assertFailsWith<BusinessException> {
            validator.validate(command(agentPublicIds = agentPublicIds))
        }

        assertEquals(ErrorCode.INVALID_REQUEST, exception.errorCode)
    }

    private fun command(
        agentPublicIds: List<UUID> = listOf(UUID.randomUUID()),
        requestedAt: LocalDateTime = LocalDateTime.of(2026, 7, 18, 15, 19, 59),
    ): BriefingRequestTask.Command =
        BriefingRequestTask.Command(
            userPublicId = UUID.randomUUID(),
            stockPublicId = UUID.randomUUID(),
            agentPublicIds = agentPublicIds,
            requestedAt = requestedAt,
        )
}
