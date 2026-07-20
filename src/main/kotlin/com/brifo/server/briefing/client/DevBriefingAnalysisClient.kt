package com.brifo.server.briefing.client

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
//@Profile("dev")
class DevBriefingAnalysisClient : BriefingAnalysisClient {
    // TODO: 외부 briefing 분석 API 구현체로 교체한다.
    override fun createBriefings(request: BriefingAnalysisClient.Request): BriefingAnalysisClient.Result =
        throw UnsupportedOperationException("외부 briefing 분석 API가 아직 연결되지 않았습니다.")
}
