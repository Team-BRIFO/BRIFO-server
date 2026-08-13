package com.brifo.server.batch.price

import com.brifo.server.stock.repository.DailyStockPriceRepository
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.service.StockPriceService
import org.slf4j.LoggerFactory
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import java.time.Duration
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
    private val requestInterval: Duration = REQUEST_INTERVAL,
    /** 테스트에서 실제로 기다리지 않도록 빼는 훅. 운영에서는 Thread.sleep. */
    private val sleeper: (Duration) -> Unit = { Thread.sleep(it.toMillis()) },
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

            // KIS 실전 계좌는 초당 20건으로 제한한다(EGW00201). 같은 앱키를 쓰는
            // 다른 트래픽(현재가 조회 등)과 공유되므로 여유를 두고 스로틀링한다.
            // 실패해도 이미 KIS 호출은 소모됐으므로 성공 여부와 무관하게 대기한다.
            sleeper(requestInterval)
        }

        return RepeatStatus.FINISHED
    }

    private companion object {
        val REQUEST_INTERVAL: Duration = Duration.ofMillis(150)
        val log = LoggerFactory.getLogger(DailyClosingPriceTasklet::class.java)
    }
}
