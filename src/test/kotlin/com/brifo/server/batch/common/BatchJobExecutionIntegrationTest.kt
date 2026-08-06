package com.brifo.server.batch.common

import com.brifo.server.ServerTestConfiguration
import com.brifo.server.batch.collection.NewsCollectionJobConfiguration
import com.brifo.server.batch.collection.NewsCollectionScheduler
import com.brifo.server.batch.generation.NewsCardGenerationJobConfiguration
import com.brifo.server.batch.generation.NewsCardGenerationScheduler
import com.brifo.server.batch.settlement.DecisionSettlementJobConfiguration
import com.brifo.server.batch.settlement.DecisionSettlementScheduler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@Import(ServerTestConfiguration::class, BatchJobExecutionIntegrationTest.FlowTestConfiguration::class)
@ActiveProfiles("test")
@SpringBootTest(properties = ["app.batch.scheduling-enabled=false"])
class BatchJobExecutionIntegrationTest @Autowired constructor(
    private val jobOperator: JobOperator,
    private val state: FlowTestState,
    private val applicationContext: ApplicationContext,
    @Qualifier(FLOW_JOB_NAME)
    private val flowJob: Job,
) {
    @BeforeEach
    fun resetState() {
        state.reset()
    }

    @Test
    fun `동일 파라미터 재시작은 성공 Step을 유지하고 실패 Step만 다시 실행한다`() {
        val targetDate = LocalDate.of(2026, 8, 3)
        val parameters = BatchJobParameters.forDate(targetDate)

        val failedExecution = jobOperator.start(flowJob, parameters)
        val restartedExecution = jobOperator.start(flowJob, parameters)

        assertEquals(BatchStatus.FAILED, failedExecution.status)
        assertEquals(BatchStatus.COMPLETED, restartedExecution.status)
        assertEquals(failedExecution.jobInstance.instanceId, restartedExecution.jobInstance.instanceId)
        assertEquals(1, state.firstStepRuns)
        assertEquals(2, state.secondStepRuns)
        assertEquals(targetDate.toString(), state.targetDate)
    }

    @Test
    fun `스케줄이 꺼져도 Job은 등록되고 Scheduler만 등록되지 않는다`() {
        assertNotNull(applicationContext.getBean(NewsCollectionJobConfiguration.JOB_NAME, Job::class.java))
        assertNotNull(applicationContext.getBean(NewsCardGenerationJobConfiguration.JOB_NAME, Job::class.java))
        assertNotNull(applicationContext.getBean(DecisionSettlementJobConfiguration.JOB_NAME, Job::class.java))
        assertFalse(applicationContext.getBeansOfType(NewsCollectionScheduler::class.java).isNotEmpty())
        assertFalse(applicationContext.getBeansOfType(NewsCardGenerationScheduler::class.java).isNotEmpty())
        assertFalse(applicationContext.getBeansOfType(DecisionSettlementScheduler::class.java).isNotEmpty())
    }

    @TestConfiguration(proxyBeanMethods = false)
    class FlowTestConfiguration {
        @Bean
        fun flowTestState() = FlowTestState()

        @Bean
        @StepScope
        fun flowFirstTasklet(
            @Value("#{jobParameters['${BatchJobParameters.TARGET_DATE}']}") targetDate: String,
            state: FlowTestState,
        ): Tasklet =
            Tasklet { _, _ ->
                state.firstStepRuns++
                state.targetDate = targetDate
                RepeatStatus.FINISHED
            }

        @Bean
        fun flowSecondTasklet(state: FlowTestState): Tasklet =
            Tasklet { _, _ ->
                state.secondStepRuns++
                check(state.secondStepRuns > 1) { "The first second-step execution fails intentionally" }
                RepeatStatus.FINISHED
            }

        @Bean
        fun flowFirstStep(
            jobRepository: JobRepository,
            transactionManager: PlatformTransactionManager,
            @Qualifier("flowFirstTasklet")
            flowFirstTasklet: Tasklet,
        ): Step =
            StepBuilder("flowFirstStep", jobRepository)
                .tasklet(flowFirstTasklet, transactionManager)
                .build()

        @Bean
        fun flowSecondStep(
            jobRepository: JobRepository,
            transactionManager: PlatformTransactionManager,
            @Qualifier("flowSecondTasklet")
            flowSecondTasklet: Tasklet,
        ): Step =
            StepBuilder("flowSecondStep", jobRepository)
                .tasklet(flowSecondTasklet, transactionManager)
                .build()

        @Bean(FLOW_JOB_NAME)
        fun flowIntegrationJob(
            jobRepository: JobRepository,
            @Qualifier("flowFirstStep")
            flowFirstStep: Step,
            @Qualifier("flowSecondStep")
            flowSecondStep: Step,
        ): Job =
            JobBuilder(FLOW_JOB_NAME, jobRepository)
                .start(flowFirstStep)
                .next(flowSecondStep)
                .build()
    }

    class FlowTestState {
        var firstStepRuns: Int = 0
        var secondStepRuns: Int = 0
        var targetDate: String? = null

        fun reset() {
            firstStepRuns = 0
            secondStepRuns = 0
            targetDate = null
        }
    }

    private companion object {
        const val FLOW_JOB_NAME = "flowIntegrationJob"
    }
}
