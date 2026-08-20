package com.brifo.server.batch.dev

import com.brifo.server.batch.common.BatchJobParameters
import com.brifo.server.batch.settlement.DecisionSettlementJobRunner
import com.brifo.server.decision.service.DecisionCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 결정 등록 직후 정산 배치를 돌린다. 이벤트를 발행할지는 `DecisionRequestService`가 판단하므로
 * (개발용 즉시 정산 설정 또는 주말 시장 모드) 여기서는 별도 조건 없이 받은 이벤트만 처리한다.
 */
@Component
@Profile("dev")
class DevDecisionSettlementListener(
    private val jobRunner: DecisionSettlementJobRunner,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun settle(event: DecisionCreatedEvent) {
        runCatching { jobRunner.start(BatchJobParameters.forDevDate(event.targetDate)) }
            .onFailure {
                log.error(
                    "개발용 즉시 결정 정산에 실패했습니다. decisionId={}, targetDate={}",
                    event.decisionPublicId,
                    event.targetDate,
                    it,
                )
            }
    }

    private companion object {
        val log = LoggerFactory.getLogger(DevDecisionSettlementListener::class.java)
    }
}
