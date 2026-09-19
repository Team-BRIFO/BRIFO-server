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
        val today = LocalDate.now(clock)
        // 카드는 전날 뉴스로 하루 한 번, 뉴스 1건당 한 장만 만들어진다. 그래서 어제 그 종목에
        // 새 뉴스가 없었으면 오늘 카드가 0장이 되고, 주말처럼 뉴스가 드문 날이 이어지면 화면이
        // 통째로 빈다. 오늘 카드가 없으면 마지막으로 만들어둔 날의 카드로 대신 채운다.
        // 카드마다 발행일을 함께 내려주므로 언제 기사인지는 화면에서 구분된다.
        val displayDate =
            newsCardRepository.findLatestDisplayDateOnOrBefore(stockPublicId, today) ?: today
        val newsCards = newsCardRepository.findDailyCards(stockPublicId, displayDate)
        // 카드가 한 장도 없는 신규 종목은 404가 아니라 빈 목록으로 응답한다.
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
                // 시세는 현재가라 카드 노출일(과거일 수 있다)이 아니라 오늘로 채운다.
                tradeDate = price.tradeDate ?: today,
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
