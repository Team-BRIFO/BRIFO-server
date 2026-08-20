package com.brifo.server.briefing.service.sync

import com.brifo.server.briefing.exception.BriefingRequestClosedException
import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.config.DevBehaviorProperties
import com.brifo.server.global.exception.BusinessException
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalTime

/** 트랜잭션 진입 전에 DB 상태와 무관한 브리핑 요청 규칙을 검증한다. */
@Component
class BriefingRequestValidator(
    private val devBehaviorProperties: DevBehaviorProperties = DevBehaviorProperties(),
) {
    /** 에이전트 선택과 요청 시각에 관한 모든 사전 요청 규칙을 검증한다. */
    fun validate(command: BriefingRequestTask.Command) {
        validateAgentSelection(command)
        validateRequestTime(command)
    }

    /** 요청 에이전트가 1명 이상 3명 이하이며 중복 선택되지 않았는지 검증한다. */
    private fun validateAgentSelection(command: BriefingRequestTask.Command) {
        val agentPublicIds = command.agentPublicIds
        if (
            agentPublicIds.isEmpty() ||
            agentPublicIds.size > MAX_AGENT_COUNT ||
            agentPublicIds.distinct().size != agentPublicIds.size
        ) {
            throw BusinessException(ErrorCode.INVALID_REQUEST)
        }
    }

    /** 요청 시각이 당일 브리핑 요청 마감 시각 전인지 검증한다. */
    private fun validateRequestTime(command: BriefingRequestTask.Command) {
        val requestedAt = command.requestedAt
        if (!devBehaviorProperties.weekendMarketEnabled && requestedAt.dayOfWeek in CLOSED_DAYS) {
            throw BriefingRequestClosedException()
        }
        if (
            devBehaviorProperties.briefingTimeRestrictionsEnabled &&
            !requestedAt.toLocalTime().isBefore(REQUEST_CUTOFF)
        ) {
            throw BriefingRequestClosedException()
        }
    }

    private companion object {
        const val MAX_AGENT_COUNT = 3
        val REQUEST_CUTOFF: LocalTime = LocalTime.of(15, 20)
        val CLOSED_DAYS = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    }
}
