package com.brifo.server.batch.settlement

import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class DecisionSettlementJobRunner(
    private val jobOperator: JobOperator,
    @Qualifier(DecisionSettlementJobConfiguration.JOB_NAME)
    private val job: Job,
) {
    @Synchronized
    fun start(
        parameters: JobParameters,
        beforeStart: () -> Unit = {},
    ): JobExecution {
        beforeStart()
        return jobOperator.start(job, parameters)
    }
}
