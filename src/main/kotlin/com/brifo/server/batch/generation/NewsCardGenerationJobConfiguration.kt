package com.brifo.server.batch.generation

import com.brifo.server.batch.common.BatchJobParameters
import com.brifo.server.batch.common.BatchProperties
import com.brifo.server.batch.common.BatchRestartListener
import com.brifo.server.batch.common.BusinessDateCalculator
import com.brifo.server.batch.common.IdListItemReader
import com.brifo.server.global.config.DevBehaviorProperties
import com.brifo.server.news.client.NewsCardGenerationClient
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.notification.repository.NotificationRepository
import com.brifo.server.notification.service.NotificationCreationService
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.term.repository.NewsCardTermRepository
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
import java.time.LocalDate

@Configuration
class NewsCardGenerationJobConfiguration {
    @Bean
    fun newsCardGenerationJob(
        jobRepository: JobRepository,
        @Qualifier("generateNewsCardStep") generateStep: Step,
        @Qualifier("createNewsCardNotificationStep") notificationStep: Step,
        restartListener: BatchRestartListener,
    ): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(generateStep)
            .next(notificationStep)
            .listener(restartListener)
            .build()

    @Bean
    fun generateNewsCardStep(
        jobRepository: JobRepository,
        @Qualifier("newsCardGenerationReader") reader: ItemReader<Long>,
        @Qualifier("newsCardGenerationProcessor") processor: ItemProcessor<Long, GeneratedNewsCardItem>,
        @Qualifier("newsCardGenerationWriter") writer: ItemWriter<GeneratedNewsCardItem>,
        transactionManager: PlatformTransactionManager,
        properties: BatchProperties,
    ): Step =
        StepBuilder("generateNewsCardStep", jobRepository)
            .chunk<Long, GeneratedNewsCardItem>(1)
            .transactionManager(transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .startLimit(properties.maxExecutions)
            .build()

    @Bean
    fun createNewsCardNotificationStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        @Qualifier("newsCardNotificationTasklet") newsCardNotificationTasklet: Tasklet,
        properties: BatchProperties,
    ): Step =
        StepBuilder("createNewsCardNotificationStep", jobRepository)
            .tasklet(newsCardNotificationTasklet, transactionManager)
            .startLimit(properties.maxExecutions)
            .build()

    @Bean
    @StepScope
    fun newsCardGenerationReader(
        @Value("#{jobParameters['${BatchJobParameters.TARGET_DATE}']}") targetDateValue: String,
        newsRepository: NewsRepository,
    ): ItemReader<Long> {
        val targetDate = LocalDate.parse(targetDateValue)
        return IdListItemReader(
            newsRepository.findGenerationCandidateIds(targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay()),
        )
    }

    @Bean
    @StepScope
    fun newsCardGenerationProcessor(
        newsRepository: NewsRepository,
        newsCardTermRepository: NewsCardTermRepository,
        client: NewsCardGenerationClient,
    ): ItemProcessor<Long, GeneratedNewsCardItem> =
        NewsCardGenerationItemProcessor(newsRepository, newsCardTermRepository, client)

    @Bean
    @StepScope
    fun newsCardGenerationWriter(
        @Value("#{jobParameters['${BatchJobParameters.TARGET_DATE}']}") targetDateValue: String,
        businessDateCalculator: BusinessDateCalculator,
        devBehaviorProperties: DevBehaviorProperties?,
        persistenceService: NewsCardPersistenceService,
    ): ItemWriter<GeneratedNewsCardItem> {
        val targetDate = LocalDate.parse(targetDateValue)
        val displayDate = if (devBehaviorProperties?.useTargetDateAsDisplayDate == true) {
            targetDate
        } else {
            businessDateCalculator.nextBusinessDay(targetDate)
        }
        return ItemWriter { chunk -> chunk.forEach { persistenceService.save(it, displayDate) } }
    }

    @Bean
    @StepScope
    fun newsCardNotificationTasklet(
        @Value("#{jobParameters['${BatchJobParameters.TARGET_DATE}']}") targetDateValue: String,
        businessDateCalculator: BusinessDateCalculator,
        devBehaviorProperties: DevBehaviorProperties?,
        newsCardRepository: NewsCardRepository,
        userStockRepository: UserStockRepository,
        notificationRepository: NotificationRepository,
        notificationCreationService: NotificationCreationService,
    ): Tasklet {
        val targetDate = LocalDate.parse(targetDateValue)
        val displayDate = if (devBehaviorProperties?.useTargetDateAsDisplayDate == true) {
            targetDate
        } else {
            businessDateCalculator.nextBusinessDay(targetDate)
        }
        return NewsCardNotificationTasklet(
            displayDate = displayDate,
            newsCardRepository = newsCardRepository,
            userStockRepository = userStockRepository,
            notificationRepository = notificationRepository,
            notificationCreationService = notificationCreationService,
        )
    }

    companion object {
        const val JOB_NAME = "newsCardGenerationJob"
    }
}
