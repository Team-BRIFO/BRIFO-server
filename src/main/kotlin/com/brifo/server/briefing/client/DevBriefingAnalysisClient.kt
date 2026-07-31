package com.brifo.server.briefing.client

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.decision.repository.DecisionResultRepository
import com.brifo.server.externalapi.ExternalApiCallPolicy
import com.brifo.server.externalapi.ExternalApiCallService
import com.brifo.server.news.repository.NewsCardRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.util.UUID

@Component
@Profile("dev")
class DevBriefingAnalysisClient(
    @Qualifier("briefingAiRestClient")
    private val restClient: RestClient,
    private val externalApiCallService: ExternalApiCallService,
    private val newsCardRepository: NewsCardRepository,
    private val agentRepository: AgentRepository,
    private val decisionResultRepository: DecisionResultRepository,
    private val transactionTemplate: TransactionTemplate,
) : BriefingAnalysisClient {
    override fun createBriefings(
        request: BriefingAnalysisClient.Request,
    ): BriefingAnalysisClient.Result {
        // ID로 AI 요청 데이터를 조회한다.
        val requestData =
            transactionTemplate.execute {
                // 카드뉴스 내용을 조회한다.
                val newsCards =
                    request.newsCardIds.map { newsCardId ->
                        val newsCard =
                            checkNotNull(
                                newsCardRepository.findByPublicId(newsCardId),
                            ) {
                                "카드뉴스를 찾을 수 없습니다: $newsCardId"
                            }

                        AiRequest.NewsCard(
                            cardId = newsCardId.toString(),
                            headline = newsCard.headline,
                            points = newsCard.points,
                        )
                    }

                // 사원 타입과 레벨을 조회한다.
                val targets =
                    request.targets.map { target ->
                        val agent =
                            checkNotNull(
                                agentRepository.findByPublicId(target.agentId),
                            ) {
                                "사원을 찾을 수 없습니다: ${target.agentId}"
                            }

                        TargetData(
                            briefingId = target.briefingId,
                            agentId = target.agentId,
                            agentType = agent.agentType,
                            level = agent.level,
                        )
                    }

                // 최근 결정과 정산 결과를 조회한다.
                val recentDecisions =
                    request.recentDecisionIds.map { decisionId ->
                        val decisionResult =
                            checkNotNull(
                                decisionResultRepository.findByDecisionPublicId(decisionId),
                            ) {
                                "결정 결과를 찾을 수 없습니다: $decisionId"
                            }

                        val decision = decisionResult.decision
                        val stock =
                            decision.briefing
                                .newsCards
                                .first()
                                .news
                                .stock

                        AiRequest.RecentDecision(
                            stockName = stock.name,
                            direction = decision.direction,
                            confidence = decision.confidenceLevel.toInt(),
                            isCorrect = decisionResult.isCorrect,
                            actualChange = decisionResult.dailyStockPrice.changeRate,
                        )
                    }

                val levels = targets.map { it.level }

                // AI 요청 형태로 조립한다.
                RequestData(
                    aiRequest =
                        AiRequest(
                            newsCard = newsCards,
                            userId = request.userId.toString(),
                            agentTypes =
                                targets
                                    .map { it.agentType }
                                    .distinct(),
                            levelRange = "${levels.min()}-${levels.max()}",
                            recentDecisions = recentDecisions,
                        ),
                    targets = targets,
                )
            } ?: error("브리핑 요청 데이터 조회에 실패했습니다.")

        // DB 조회가 끝난 후 AI 서버를 호출한다.
        val response =
            externalApiCallService.execute(
                provider = "AI",
                apiName = "CREATE_BRIEFINGS",
                policy = ExternalApiCallPolicy.AI_BRIEFING,

                // 조립한 요청을 외부 API 로그에 저장한다.
                requestPayload = requestData.aiRequest,
            ) {
                restClient
                    .post()
                    .uri("/ai/briefing/generate")
                    .body(requestData.aiRequest)
                    .retrieve()
                    .toEntity(AiResponse::class.java)
            }

        // AI가 실패 결과를 반환하면 예외로 처리한다.
        check(response.isSuccess) {
            "브리핑 생성에 실패했습니다: ${response.message}"
        }

        // 성공 응답에는 result가 필요하다.
        val result =
            checkNotNull(response.result) {
                "브리핑 생성 결과가 없습니다."
            }

        // agentType으로 내부 ID를 다시 연결한다.
        val briefings =
            result.briefings.map { briefing ->
                val target =
                    requestData.targets.single {
                        it.agentType == briefing.agentType
                    }

                BriefingAnalysisClient.BriefingResult(
                    briefingId = target.briefingId,
                    agentId = target.agentId,
                    agentType = briefing.agentType,
                    direction = briefing.direction,

                    // 75를 내부 형식인 0.75로 변환한다.
                    probability =
                        briefing.confidenceRate
                            .toBigDecimal()
                            .movePointLeft(2),

                    headline = briefing.headline,
                    summary = briefing.summary,
                    personalComment = briefing.personalComment,

                    // AI 응답 필드를 기존 인터페이스에 맞춘다.
                    commonAnalysis = briefing.contentText,
                    closingComment = briefing.oneLiner,

                    modelName = briefing.modelName,
                    cached = briefing.cached,
                    personalCached = briefing.personalCached,
                )
            }

        // 기존 인터페이스 형식으로 반환한다.
        return BriefingAnalysisClient.Result(
            briefings = briefings,
        )
    }

    private data class RequestData(
        val aiRequest: AiRequest,
        val targets: List<TargetData>,
    )

    private data class TargetData(
        val briefingId: UUID,
        val agentId: UUID,
        val agentType: AgentType,
        val level: Int,
    )

    private data class AiRequest(
        val newsCard: List<NewsCard>,
        val userId: String,
        val agentTypes: List<AgentType>,
        val levelRange: String,
        val recentDecisions: List<RecentDecision>,
    ) {
        data class NewsCard(
            val cardId: String,
            val headline: String,
            val points: List<String>,
        )

        data class RecentDecision(
            val stockName: String,
            val direction: DecisionDirection,
            val confidence: Int,
            val isCorrect: Boolean,
            val actualChange: BigDecimal,
        )
    }

    private data class AiResponse(
        val isSuccess: Boolean,
        val code: String,
        val message: String,
        val result: Result?,
    ) {
        data class Result(
            val briefings: List<Briefing>,
        )

        data class Briefing(
            val agentType: AgentType,
            val direction: BriefingDirection,
            val confidenceRate: Int,
            val headline: String,
            val summary: String,
            val contentText: String,
            val oneLiner: String,
            val modelName: String,
            val cached: Boolean,
            val personalComment: String?,
            val personalCached: Boolean,
        )
    }
}
