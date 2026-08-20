package com.brifo.server.news.service

import com.brifo.server.news.dto.response.GetNewsCardsResponse
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.stock.exception.StockNotFoundException
import com.brifo.server.stock.repository.StockRepository
import com.brifo.server.stock.service.StockPriceService
import com.brifo.server.term.repository.NewsCardTermRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class NewsService(
    private val newsCardRepository: NewsCardRepository,
    private val newsCardTermRepository: NewsCardTermRepository,
    private val stockRepository: StockRepository,
    private val stockPriceService: StockPriceService,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getNewsCards(stockPublicId: UUID): GetNewsCardsResponse {
        val displayDate = LocalDate.now(clock)
        val newsCards = newsCardRepository.findDisplayCards(stockPublicId, displayDate)
        // 오늘 카드가 아직 생성되지 않은 것은 정상 상태이므로 404가 아니라 빈 목록으로 응답한다.
        // 종목 정보는 카드가 없어도 내려줘야 프론트가 헤더와 빈 상태를 함께 그릴 수 있다.
        val stock = newsCards.firstOrNull()?.news?.stock
            ?: stockRepository.findByPublicId(stockPublicId)
            ?: throw StockNotFoundException()

        val price = stockPriceService.getCurrentPrice(requireNotNull(stock.id), stock.code)

        val stockResponse =
            GetNewsCardsResponse.NewsStock(
                stockId = requireNotNull(stock.publicId),
                name = stock.name,
                sector = stock.sector,
                logoUrl = stock.logoUrl,
                price = price.currentPrice,
                changeRate = (price.changeRate ?: BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP),
                tradeDate = price.tradeDate ?: displayDate,
            )

        val newsCardResponses = newsCards.map { newsCard ->
            val news = newsCard.news
            val terms = newsCardTermRepository.findAllByNewsCardIdOrderByDisplayOrderAsc(
                requireNotNull(newsCard.id),
            )
            GetNewsCardsResponse.StockNewsCard(
                cardId = requireNotNull(newsCard.publicId),
                source = news.source,
                headline = newsCard.headline,
                importanceBadge = newsCard.importanceBadge,
                publishedDate = news.publishedAt.atZone(SEOUL_ZONE).toInstant(),
                imageUrl = newsCard.imageUrl,
                points = newsCard.points,
                keywords = newsCard.keywords,
                terms = terms.map {
                    GetNewsCardsResponse.Term(
                        termId = requireNotNull(it.term.publicId),
                        surface = it.surface ?: it.term.term,
                        displayOrder = it.displayOrder,
                    )
                },
            )
        }

        return GetNewsCardsResponse(
            stock = stockResponse,
            newsCards = newsCardResponses,
        )
    }

    private companion object {
        val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
