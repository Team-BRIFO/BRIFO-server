package com.brifo.server.batch.price

import com.brifo.server.stock.repository.DailyStockPriceRepository
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.service.StockPriceService
import org.slf4j.LoggerFactory
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import java.time.LocalDate

/**
 * 화면 표시용(종목 목록/검색) 종가를 채운다.
 * 정산 배치(ClosingPriceTasklet, is_closing=true)와 달리 실패해도 전체를 막지 않고 종목별로 건너뛴다.
 */
class DailyClosingPriceTasklet(
    private val targetDate: LocalDate,
    private val stockRepository: StockRepository,
    private val dailyStockPriceRepository: DailyStockPriceRepository,
    private val stockPriceService: StockPriceService,
) : Tasklet {
    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val stocks = stockRepository.findAllByIsActiveTrueOrderByCode()

        stocks.forEach { stock ->
            val stockId = requireNotNull(stock.id)

            if (dailyStockPriceRepository.findByStockIdAndTradeDateAndIsClosingFalse(stockId, targetDate) != null) {
                return@forEach
            }

            runCatching {
                stockPriceService.saveClosingPrice(stock, targetDate)
            }.onFailure { exception ->
                log.warn(
                    "일별 종가 백필에 실패했습니다. stockId={}, stockCode={}, targetDate={}",
                    stockId,
                    stock.code,
                    targetDate,
                    exception,
                )
            }
        }

        return RepeatStatus.FINISHED
    }

    private companion object {
        val log = LoggerFactory.getLogger(DailyClosingPriceTasklet::class.java)
    }
}
