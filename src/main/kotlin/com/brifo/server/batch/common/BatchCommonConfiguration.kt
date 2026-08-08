package com.brifo.server.batch.common

import org.springframework.batch.core.configuration.JobRegistry
import org.springframework.batch.core.configuration.support.MapJobRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
@EnableConfigurationProperties(BatchProperties::class)
class BatchCommonConfiguration {
    @Bean
    fun jobRegistry(): JobRegistry = MapJobRegistry()

    @Bean
    fun batchTaskScheduler(): TaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = 2
            setThreadNamePrefix("batch-restart-")
        }
}
