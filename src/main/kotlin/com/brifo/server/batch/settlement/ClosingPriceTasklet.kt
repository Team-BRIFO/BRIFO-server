package com.brifo.server.batch.settlement

import com.brifo.server.decision.DecisionMarketPolicy
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.stock.client.ClosingPriceClient
import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.repository.DailyStockPriceRepository
import com.brifo.server.stock.repository.StockRepository
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import java.time.LocalDate

class ClosingPriceTasklet(
    private val targetDate: LocalDate,
    private val decisionRepository: DecisionRepository,
    private val stockRepository: StockRepository,
    private val dailyStockPriceRepository: DailyStockPriceRepository,
    private val client: ClosingPriceClient,
    private val ignoreSettlementCutoff: Boolean = false,
) : Tasklet {
    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val stockIds = decisionRepository.findUnsettledStockIds(targetDate, ignoreSettlementCutoff)
        stockRepository.findAllById(stockIds).sortedBy { it.id }.forEach { stock ->
            val stockId = requireNotNull(stock.id)
            if (dailyStockPriceRepository.findByStockIdAndTradeDateAndIsClosingTrue(stockId, targetDate) == null) {
                val closingPrice = resolveClosingPrice(stock, stockId)
                dailyStockPriceRepository.save(
                    DailyStockPrice.createClosing(
                        stock = stock,
                        tradeDate = targetDate,
                        price = closingPrice.price,
                        changeRate = closingPrice.changeRate,
                    ),
                )
            }
        }
        return RepeatStatus.FINISHED
    }

    /**
     * 휴장일에는 그날의 종가가 존재하지 않으므로 직전 거래일 시세를 그날의 종가로 본다.
     * 데모데이 주말 시연을 위한 경로이고, prod에서는 도달하지 않는다 — 정산 스케줄러가
     * `MON-FRI`로만 돌고 주말 예측 등록 자체가 막혀 있다.
     *
     * 평일에는 기존대로 외부에서 그날 종가를 조회한다. 조회 실패 시 직전 시세로 대체하는
     * 폴백은 일부러 넣지 않는다 — 낡은 시세로 조용히 정산되는 쪽이 실패보다 위험하다.
     */
    private fun resolveClosingPrice(
        stock: Stock,
        stockId: Long,
    ): ClosingPriceClient.Result {
        if (DecisionMarketPolicy.isMarketOpenOn(targetDate)) {
            return client.getClosingPrice(ClosingPriceClient.Request(stockId, stock.code, targetDate))
        }
        val previousClose = dailyStockPriceRepository
            .findTopByStockIdAndTradeDateBeforeOrderByTradeDateDesc(stockId, targetDate)
            ?: error(
                "휴장일 정산에 사용할 직전 거래일 시세가 없습니다. " +
                    "stockId=$stockId, stockCode=${stock.code}, targetDate=$targetDate",
            )
        return ClosingPriceClient.Result(
            price = previousClose.price,
            changeRate = previousClose.changeRate,
        )
    }
}
