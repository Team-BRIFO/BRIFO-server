package com.brifo.server.batch.settlement

import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import java.time.Clock
import java.time.LocalDateTime

class PendingUserStockApplicationTasklet(
    private val service: PendingUserStockApplicationService,
    private val clock: Clock,
) : Tasklet {
    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val effectiveAt = LocalDateTime.now(clock)
        service.findTargetUserIds(effectiveAt).forEach { userId ->
            service.applyUser(userId, effectiveAt)
        }
        return RepeatStatus.FINISHED
    }
}
