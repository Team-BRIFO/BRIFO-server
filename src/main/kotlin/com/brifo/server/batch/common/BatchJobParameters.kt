package com.brifo.server.batch.common

import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import java.time.LocalDate

object BatchJobParameters {
    const val TARGET_DATE = "targetDate"
    const val COLLECTION_ROUND = "collectionRound"
    const val DEV_RUN_ID = "devRunId"
    const val IGNORE_SETTLEMENT_CUTOFF = "ignoreSettlementCutoff"

    fun forDate(targetDate: LocalDate): JobParameters =
        JobParametersBuilder()
            .addString(TARGET_DATE, targetDate.toString(), true)
            .addString(IGNORE_SETTLEMENT_CUTOFF, false.toString(), false)
            .toJobParameters()

    fun forCollection(
        targetDate: LocalDate,
        collectionRound: String,
    ): JobParameters =
        JobParametersBuilder()
            .addString(TARGET_DATE, targetDate.toString(), true)
            .addString(COLLECTION_ROUND, collectionRound, true)
            .toJobParameters()

    fun forDevDate(targetDate: LocalDate): JobParameters =
        JobParametersBuilder()
            .addString(TARGET_DATE, targetDate.toString(), true)
            .addString(IGNORE_SETTLEMENT_CUTOFF, true.toString(), false)
            .addString(DEV_RUN_ID, java.util.UUID.randomUUID().toString(), true)
            .toJobParameters()

    fun forDevCollection(
        targetDate: LocalDate,
        collectionRound: String,
    ): JobParameters =
        JobParametersBuilder()
            .addString(TARGET_DATE, targetDate.toString(), true)
            .addString(COLLECTION_ROUND, collectionRound, true)
            .addString(DEV_RUN_ID, java.util.UUID.randomUUID().toString(), true)
            .toJobParameters()
}
