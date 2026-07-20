package com.brifo.server.briefing.support

import com.brifo.server.briefing.client.BriefingAnalysisClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration(proxyBeanMethods = false)
class BriefingTestConfiguration {
    @Bean
    fun briefingAnalysisClient(): BriefingAnalysisClient =
        object : BriefingAnalysisClient {
            override fun createBriefings(request: BriefingAnalysisClient.Request) =
                throw IllegalStateException("테스트용 브리핑 분석 실패")
        }
}
