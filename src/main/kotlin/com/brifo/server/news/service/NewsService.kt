package com.brifo.server.news.service

import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.exception.BusinessException
import com.brifo.server.news.dto.response.GetNewsCardResponse
import com.brifo.server.news.exception.NewsCardNotFoundException
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.news.repository.NewsDailyStockPriceRepository
import com.brifo.server.term.repository.NewsCardTermRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.time.ZoneId
import java.util.UUID

@Service
class NewsService(
    private val newsCardRepository: NewsCardRepository,
    private val newsDailyStockPriceRepository: NewsDailyStockPriceRepository,
    private val newsCardTermRepository: NewsCardTermRepository,
) {
    @Transactional(readOnly = true)
    fun getNewsCard(cardId: UUID): GetNewsCardResponse {
        val newsCard = newsCardRepository.findByPublicId(cardId) ?: throw NewsCardNotFoundException()
        val news = newsCard.news
        val stock = news.stock
        val publishedDate = news.publishedAt.atZone(SEOUL_ZONE_ID).toLocalDate()

        val price =
            newsDailyStockPriceRepository.findTopByStockIdAndTradeDateLessThanEqualOrderByTradeDateDesc(
                stockId = requireNotNull(stock.id),
                tradeDate = publishedDate,
            ) ?: throw BusinessException(
                errorCode = ErrorCode.INTERNAL_SERVER_ERROR,
                message = "카드뉴스 기준일의 종목 가격 데이터가 없습니다.",
            )

        val terms =
            newsCardTermRepository.findAllByNewsCardIdOrderByDisplayOrderAsc(
                requireNotNull(newsCard.id),
            )

        val stockResponse =
            GetNewsCardResponse.Stock(
                stockId = requireNotNull(stock.publicId),
                name = stock.name,
                sector = stock.sector,
                price = price.price,
                changeRate = price.changeRate.setScale(1, RoundingMode.HALF_UP),
                tradeDate = price.tradeDate,
            )

        val termResponses =
            terms.map {
                GetNewsCardResponse.Term(
                    termId = requireNotNull(it.term.publicId),
                    surface = it.surface ?: it.term.term,
                    displayOrder = it.displayOrder,
                )
            }

        val newsCardResponse =
            GetNewsCardResponse.NewsCard(
                cardId = requireNotNull(newsCard.publicId),
                source = news.source,
                headline = newsCard.headline,
                importanceBadge = newsCard.importanceBadge,
                publishedDate = publishedDate,
                points = newsCard.points,
                keywords = newsCard.keywords,
                terms = termResponses,
            )

        return GetNewsCardResponse(
            stock = stockResponse,
            newsCard = listOf(newsCardResponse),
        )
    }

    private companion object {
        val SEOUL_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
