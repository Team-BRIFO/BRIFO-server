package com.brifo.server.batch.price

import com.brifo.server.batch.common.BatchJobParameters
import com.brifo.server.batch.common.BatchProperties
import com.brifo.server.batch.common.BatchRestartListener
import com.brifo.server.stock.repository.DailyStockPriceRepository
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.service.StockPriceService
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate

@Configuration
class DailyClosingPriceJobConfiguration {
    @Bean
    fun dailyClosingPriceJob(
        jobRepository: JobRepository,
        @Qualifier("fetchDailyClosingPriceStep") fetchDailyClosingPriceStep: Step,
        restartListener: BatchRestartListener,
    ): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(fetchDailyClosingPriceStep)
            .listener(restartListener)
            .build()

    @Bean
    fun fetchDailyClosingPriceStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        dailyClosingPriceTasklet: Tasklet,
        properties: BatchProperties,
    ): Step =
        StepBuilder("fetchDailyClosingPriceStep", jobRepository)
            .tasklet(dailyClosingPriceTasklet, transactionManager)
            .startLimit(properties.maxExecutions)
            .build()

    @Bean
    @StepScope
    fun dailyClosingPriceTasklet(
        @Value("#{jobParameters['${BatchJobParameters.TARGET_DATE}']}") targetDate: String,
        stockRepository: StockRepository,
        dailyStockPriceRepository: DailyStockPriceRepository,
        stockPriceService: StockPriceService,
    ): Tasklet =
        DailyClosingPriceTasklet(
            targetDate = LocalDate.parse(targetDate),
            stockRepository = stockRepository,
            dailyStockPriceRepository = dailyStockPriceRepository,
            stockPriceService = stockPriceService,
        )

    companion object {
        const val JOB_NAME = "dailyClosingPriceJob"
    }
}
