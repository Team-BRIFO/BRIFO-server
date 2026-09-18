package com.brifo.server.decision.service

import com.brifo.server.decision.dto.response.GetDecisionResultResponse
import com.brifo.server.decision.dto.response.GetDecisionsResponse
import com.brifo.server.decision.exception.DecisionNotFoundException
import com.brifo.server.decision.exception.DecisionNotSettledException
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.decision.repository.DecisionResultRepository
import com.brifo.server.decision.repository.TodayDecisionRow
import com.brifo.server.global.exception.BusinessException
import com.brifo.server.stock.code.StockErrorCode
import com.brifo.server.stock.service.StockPriceService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

/** 오늘의 결정 목록과 정산 완료 결과를 조회한다. */
@Service
class DecisionQueryService(
    private val decisionRepository: DecisionRepository,
    private val decisionResultRepository: DecisionResultRepository,
    private val stockPriceService: StockPriceService,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getDecisions(userPublicId: UUID): GetDecisionsResponse =
        GetDecisionsResponse(
            items = decisionRepository.findTodayDecisions(
                userPublicId = userPublicId,
                displayDate = LocalDate.now(clock),
            ).map(::toDecisionItem),
        )

    /**
     * 정산 전 종목은 장중 실시간 현재가(최대 1분 지연, [StockPriceService] 캐시)로 덮어써
     * 오늘의 예측 화면에서 시세가 계속 움직이는 것처럼 보이게 한다. 정산된 결정은 정산
     * 시점의 종가를 그대로 유지한다. 실시간 조회 실패 시 배치로 저장된 값을 그대로 쓴다.
     */
    private fun toDecisionItem(row: TodayDecisionRow): GetDecisionsResponse.DecisionItem {
        var price = row.price
        var changeRate = row.changeRate
        var tradeDate = row.tradeDate

        if (!row.isSettled) {
            val currentPrice = runCatching {
                stockPriceService.getCurrentPrice(row.stockId, row.stockCode)
            }.getOrElse { exception ->
                if (exception is BusinessException && exception.errorCode == StockErrorCode.STOCK_PRICE_UNAVAILABLE) {
                    null
                } else {
                    throw exception
                }
            }
            if (currentPrice != null) {
                price = currentPrice.currentPrice.toLong()
                changeRate = currentPrice.changeRate
                tradeDate = currentPrice.tradeDate ?: tradeDate
            }
        }

        return GetDecisionsResponse.DecisionItem(
            decisionId = row.decisionId,
            direction = row.direction,
            allocatedAp = row.allocatedAp,
            isSettled = row.isSettled,
            agent = GetDecisionsResponse.DecisionListAgent(row.agentId, row.agentType),
            stock = GetDecisionsResponse.DecisionListStock(
                stockId = row.stockPublicId,
                name = row.stockName,
                logoUrl = row.logoUrl,
                price = price,
                changeRate = changeRate,
                tradeDate = tradeDate,
            ),
        )
    }

    @Transactional(readOnly = true)
    fun getDecisionResult(
        userPublicId: UUID,
        decisionPublicId: UUID,
    ): GetDecisionResultResponse {
        if (
            !decisionRepository.existsByPublicIdAndBriefingAgentUserPublicId(
                decisionPublicId,
                userPublicId,
            )
        ) {
            throw DecisionNotFoundException()
        }
        if (!decisionResultRepository.existsByDecisionPublicId(decisionPublicId)) {
            throw DecisionNotSettledException()
        }
        val results = decisionRepository.findDecisionResults(userPublicId, decisionPublicId)
        check(results.size == 1) {
            "Decision settlement must have exactly one AP transaction: $decisionPublicId"
        }
        return results.single()
    }
}
