package com.brifo.server.news.service

import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.exception.BusinessException
import com.brifo.server.news.dto.response.GetNewsCardsResponse
import com.brifo.server.news.exception.NewsCardNotFoundException
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.news.repository.NewsDailyStockPriceRepository
import com.brifo.server.term.repository.NewsCardTermRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
class NewsService(
    private val newsCardRepository: NewsCardRepository,
    private val newsDailyStockPriceRepository: NewsDailyStockPriceRepository,
    private val newsCardTermRepository: NewsCardTermRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getNewsCards(stockPublicId: UUID): GetNewsCardsResponse {
        val displayDate = LocalDate.now(clock)
        val newsCards = newsCardRepository.findAnalysisCards(stockPublicId, displayDate)
        if (newsCards.size !in MIN_REQUIRED_NEWS_CARD_COUNT..MAX_REQUIRED_NEWS_CARD_COUNT) {
            throw NewsCardNotFoundException()
        }
        val stock = newsCards.first().news.stock

        val price =
            newsDailyStockPriceRepository.findTopByStockIdAndTradeDateLessThanEqualOrderByTradeDateDescFetchedAtDescIdDesc(
                stockId = requireNotNull(stock.id),
                tradeDate = displayDate,
            ) ?: throw BusinessException(
                errorCode = ErrorCode.INTERNAL_SERVER_ERROR,
                message = "카드뉴스 기준일의 종목 가격 데이터가 없습니다.",
            )

        val stockResponse =
            GetNewsCardsResponse.NewsStock(
                stockId = requireNotNull(stock.publicId),
                name = stock.name,
                sector = stock.sector,
                price = price.price,
                changeRate = price.changeRate.setScale(1, RoundingMode.HALF_UP),
                tradeDate = price.tradeDate,
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
                publishedDate = news.publishedAt.toLocalDate(),
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
        const val MIN_REQUIRED_NEWS_CARD_COUNT = 1
        const val MAX_REQUIRED_NEWS_CARD_COUNT = 2
    }
}
