package com.brifo.server.batch.settlement

import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.stock.repository.DailyStockPriceRepository
import org.springframework.batch.infrastructure.item.ItemProcessor
import java.time.LocalDate

class DecisionSettlementItemProcessor(
    private val targetDate: LocalDate,
    private val decisionRepository: DecisionRepository,
    private val dailyStockPriceRepository: DailyStockPriceRepository,
    private val calculator: DecisionSettlementCalculator,
) : ItemProcessor<Long, DecisionSettlementItem> {
    override fun process(decisionId: Long): DecisionSettlementItem {
        val decision = decisionRepository.findById(decisionId).orElseThrow {
            IllegalStateException("결정을 찾을 수 없습니다: $decisionId")
        }
        val stock = decision.briefing.newsCards.first().news.stock
        val price = dailyStockPriceRepository.findByStockIdAndTradeDateAndIsClosingTrue(
            requireNotNull(stock.id),
            targetDate,
        ) ?: error("종가를 찾을 수 없습니다. stockId=${stock.id}, targetDate=$targetDate")
        val actualDirection = calculator.actualDirection(price.changeRate)
        return DecisionSettlementItem(
            decisionId = decisionId,
            dailyStockPriceId = requireNotNull(price.id),
            isCorrect = decision.direction == actualDirection,
        )
    }
}
