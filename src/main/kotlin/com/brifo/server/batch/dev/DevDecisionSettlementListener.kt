package com.brifo.server.batch.dev

import com.brifo.server.batch.common.BatchJobParameters
import com.brifo.server.batch.settlement.DecisionSettlementJobRunner
import com.brifo.server.decision.service.DecisionCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
@Profile("dev")
@ConditionalOnProperty(
    prefix = "app.dev-behavior",
    name = ["immediate-decision-settlement"],
    havingValue = "true",
)
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
