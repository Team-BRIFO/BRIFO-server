package com.brifo.server.batch.settlement

import com.brifo.server.batch.common.BatchJobParameters
import com.brifo.server.batch.common.BatchProperties
import com.brifo.server.batch.common.BatchRestartListener
import com.brifo.server.batch.common.IdListItemReader
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.stock.client.ClosingPriceClient
import com.brifo.server.stock.repository.DailyStockPriceRepository
import com.brifo.server.stock.repository.StockRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.Clock
import java.time.LocalDate

@Configuration
class DecisionSettlementJobConfiguration {
    @Bean
    fun decisionSettlementJob(
        jobRepository: JobRepository,
        @Qualifier("fetchClosingPriceStep") closingPriceStep: Step,
        @Qualifier("settleDecisionStep") settlementStep: Step,
        @Qualifier("applyPendingUserStockStep") applyPendingUserStockStep: Step,
        restartListener: BatchRestartListener,
    ): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(closingPriceStep)
            .next(settlementStep)
            .next(applyPendingUserStockStep)
            .listener(restartListener)
            .build()

    @Bean
    fun fetchClosingPriceStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        closingPriceTasklet: Tasklet,
        properties: BatchProperties,
    ): Step =
        StepBuilder("fetchClosingPriceStep", jobRepository)
            .tasklet(closingPriceTasklet, transactionManager)
            .startLimit(properties.maxExecutions)
            .build()

    @Bean
    fun settleDecisionStep(
        jobRepository: JobRepository,
        @Qualifier("decisionSettlementReader") reader: ItemReader<Long>,
        processor: ItemProcessor<Long, DecisionSettlementItem>,
        @Qualifier("decisionSettlementWriter") writer: ItemWriter<DecisionSettlementItem>,
        properties: BatchProperties,
    ): Step =
        StepBuilder("settleDecisionStep", jobRepository)
            .chunk<Long, DecisionSettlementItem>(1)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .startLimit(properties.maxExecutions)
            .build()

    @Bean
    fun applyPendingUserStockStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        pendingUserStockApplicationTasklet: PendingUserStockApplicationTasklet,
        properties: BatchProperties,
    ): Step =
        StepBuilder("applyPendingUserStockStep", jobRepository)
            .tasklet(pendingUserStockApplicationTasklet, transactionManager)
            .startLimit(properties.maxExecutions)
            .build()

    @Bean
    fun pendingUserStockApplicationTasklet(
        service: PendingUserStockApplicationService,
        clock: Clock,
    ): PendingUserStockApplicationTasklet = PendingUserStockApplicationTasklet(service, clock)

    @Bean
    @StepScope
    fun closingPriceTasklet(
        @Value("#{jobParameters['${BatchJobParameters.TARGET_DATE}']}") targetDateValue: String,
        decisionRepository: DecisionRepository,
        stockRepository: StockRepository,
        dailyStockPriceRepository: DailyStockPriceRepository,
        client: ClosingPriceClient,
    ): Tasklet = ClosingPriceTasklet(
        targetDate = LocalDate.parse(targetDateValue),
        decisionRepository = decisionRepository,
        stockRepository = stockRepository,
        dailyStockPriceRepository = dailyStockPriceRepository,
        client = client,
    )

    @Bean
    @StepScope
    fun decisionSettlementReader(
        @Value("#{jobParameters['${BatchJobParameters.TARGET_DATE}']}") targetDateValue: String,
        decisionRepository: DecisionRepository,
    ): ItemReader<Long> = IdListItemReader(decisionRepository.findUnsettledIds(LocalDate.parse(targetDateValue)))

    @Bean
    @StepScope
    fun decisionSettlementProcessor(
        @Value("#{jobParameters['${BatchJobParameters.TARGET_DATE}']}") targetDateValue: String,
        decisionRepository: DecisionRepository,
        dailyStockPriceRepository: DailyStockPriceRepository,
        calculator: DecisionSettlementCalculator,
    ): ItemProcessor<Long, DecisionSettlementItem> = DecisionSettlementItemProcessor(
        targetDate = LocalDate.parse(targetDateValue),
        decisionRepository = decisionRepository,
        dailyStockPriceRepository = dailyStockPriceRepository,
        calculator = calculator,
    )

    @Bean
    @StepScope
    fun decisionSettlementWriter(settlementService: DecisionSettlementService): ItemWriter<DecisionSettlementItem> =
        ItemWriter { chunk -> chunk.forEach(settlementService::settle) }

    companion object {
        const val JOB_NAME = "decisionSettlementJob"
    }
}
