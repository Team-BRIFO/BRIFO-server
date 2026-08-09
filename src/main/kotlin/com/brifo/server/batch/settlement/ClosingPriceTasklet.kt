package com.brifo.server.batch.settlement

import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.stock.client.ClosingPriceClient
import com.brifo.server.stock.entity.DailyStockPrice
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
) : Tasklet {
    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val stockIds = decisionRepository.findUnsettledStockIds(targetDate)
        stockRepository.findAllById(stockIds).sortedBy { it.id }.forEach { stock ->
            val stockId = requireNotNull(stock.id)
            if (dailyStockPriceRepository.findByStockIdAndTradeDateAndIsClosingTrue(stockId, targetDate) == null) {
                val closingPrice =
                    client.getClosingPrice(ClosingPriceClient.Request(stockId, stock.code, targetDate))
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
}
