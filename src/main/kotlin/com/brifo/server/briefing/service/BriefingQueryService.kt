package com.brifo.server.briefing.service

import com.brifo.server.briefing.dto.response.BriefingStockResponse
import com.brifo.server.briefing.dto.response.GetBriefingDetailResponse
import com.brifo.server.briefing.dto.response.GetOfficeBriefingsResponse
import com.brifo.server.briefing.dto.response.GetStockBriefingsResponse
import com.brifo.server.briefing.entity.BriefingStatus
import com.brifo.server.briefing.exception.BriefingNotCompletedException
import com.brifo.server.briefing.exception.BriefingNotFoundException
import com.brifo.server.briefing.exception.BriefingProcessingFailedException
import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.stock.exception.StockNotFoundException
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.stock.service.StockPriceService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

/** 브리핑 조회 유스케이스를 처리한다. */
@Service
class BriefingQueryService(
    private val briefingRepository: BriefingRepository,
    private val userStockRepository: UserStockRepository,
    private val stockRepository: StockRepository,
    private val stockPriceService: StockPriceService,
    private val clock: Clock,
) {
    /** 오늘 해당 종목에 요청한 에이전트별 브리핑 전체를 조회한다. */
    @Transactional(readOnly = true)
    fun getStockBriefings(
        userPublicId: UUID,
        stockPublicId: UUID,
    ): GetStockBriefingsResponse {
        if (!userStockRepository.existsInterestStock(userPublicId, stockPublicId)) {
            throw StockNotFoundException()
        }
        val items = briefingRepository.findStockBriefingItems(
            userPublicId = userPublicId,
            stockPublicId = stockPublicId,
            displayDate = LocalDate.now(clock),
        )
        if (items.isEmpty()) {
            throw BriefingNotFoundException()
        }
        val stockEntity = stockRepository.findByPublicId(stockPublicId) ?: throw StockNotFoundException()
        val price = stockPriceService.getCurrentPrice(requireNotNull(stockEntity.id), stockEntity.code)
        val stock =
            BriefingStockResponse(
                stockId = stockPublicId,
                name = stockEntity.name,
                logoUrl = stockEntity.logoUrl,
                price = price.currentPrice,
                changeRate = (price.changeRate ?: BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP),
                tradeDate = price.tradeDate ?: LocalDate.now(clock),
            )

        return GetStockBriefingsResponse(stock = stock, items = items)
    }

    /** 오늘 사용자의 종목별 브리핑을 오피스 화면 형태로 조회한다. */
    @Transactional(readOnly = true)
    fun getOfficeBriefings(userPublicId: UUID): GetOfficeBriefingsResponse =
        GetOfficeBriefingsResponse(
            items = briefingRepository.findOfficeBriefings(
                userPublicId = userPublicId,
                displayDate = LocalDate.now(clock),
            ),
        )

    /** 사용자가 소유한 완료 브리핑의 에이전트, 뉴스 카드와 분석 결과를 조회한다. */
    @Transactional(readOnly = true)
    fun getBriefingDetail(
        userPublicId: UUID,
        briefingPublicId: UUID,
    ): GetBriefingDetailResponse {
        val briefing = briefingRepository.findOwnedBriefing(userPublicId, briefingPublicId)
            ?: throw BriefingNotFoundException()
        when (briefing.status) {
            BriefingStatus.PENDING,
            BriefingStatus.ANALYZING,
            -> throw BriefingNotCompletedException()
            BriefingStatus.FAILED -> throw BriefingProcessingFailedException()
            BriefingStatus.COMPLETED -> Unit
        }

        val stockEntity = briefing.newsCards.first().news.stock
        val stockPublicId = stockEntity.publicId!!
        val stock = briefingRepository.findStockSummary(stockPublicId)
            ?: error("Latest stock price is missing for stock $stockPublicId")

        return GetBriefingDetailResponse(
            stock = stock,
            agent = GetBriefingDetailResponse.BriefingDetailAgent(
                agentId = briefing.agent.publicId!!,
                agentType = briefing.agent.agentType,
                nickname = briefing.agent.nickname,
                modelName = briefing.agent.modelName,
            ),
            newsCards = briefing.newsCards.map { newsCard ->
                GetBriefingDetailResponse.BriefingNewsCard(
                    cardId = newsCard.publicId!!,
                    headline = newsCard.headline,
                )
            },
            briefing = GetBriefingDetailResponse.BriefingDetailContent(
                briefingId = briefing.publicId!!,
                direction = checkNotNull(briefing.direction),
                confidenceRate = checkNotNull(briefing.confidenceRate).toInt(),
                summary = checkNotNull(briefing.summary),
                personalComment = briefing.personalComment,
                contentText = checkNotNull(briefing.contentText),
                oneLiner = checkNotNull(briefing.oneLiner),
            ),
        )
    }
}
