package com.brifo.server.batch.collection

import com.brifo.server.batch.common.BatchJobParameters
import com.brifo.server.batch.common.BatchProperties
import com.brifo.server.batch.common.BatchRestartListener
import com.brifo.server.news.client.NewsCollectionClient
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.stock.repository.StockRepository
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
class NewsCollectionJobConfiguration {
    @Bean
    fun newsCollectionJob(
        jobRepository: JobRepository,
        @Qualifier("collectNewsStep") collectNewsStep: Step,
        restartListener: BatchRestartListener,
    ): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(collectNewsStep)
            .listener(restartListener)
            .build()

    @Bean
    fun collectNewsStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        newsCollectionTasklet: Tasklet,
        properties: BatchProperties,
    ): Step =
        StepBuilder("collectNewsStep", jobRepository)
            .tasklet(newsCollectionTasklet, transactionManager)
            .startLimit(properties.maxExecutions)
            .build()

    @Bean
    @StepScope
    fun newsCollectionTasklet(
        @Value("#{jobParameters['${BatchJobParameters.TARGET_DATE}']}") targetDate: String,
        @Value("#{jobParameters['${BatchJobParameters.COLLECTION_ROUND}']}") collectionRound: String,
        client: NewsCollectionClient,
        stockRepository: StockRepository,
        newsRepository: NewsRepository,
        importanceCalculator: NewsImportanceCalculator,
    ): Tasklet =
        NewsCollectionTasklet(
            targetDate = LocalDate.parse(targetDate),
            collectionRound = CollectionRound.valueOf(collectionRound),
            client = client,
            stockRepository = stockRepository,
            newsRepository = newsRepository,
            importanceCalculator = importanceCalculator,
        )

    companion object {
        const val JOB_NAME = "newsCollectionJob"
    }
}
