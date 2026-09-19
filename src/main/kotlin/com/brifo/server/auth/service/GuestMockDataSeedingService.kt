package com.brifo.server.auth.service

import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.ap.entity.ApTransactionReason
import com.brifo.server.ap.entity.ApTransactionTargetType
import com.brifo.server.ap.service.ApTransactionService
import com.brifo.server.badge.code.BadgeCode
import com.brifo.server.badge.service.BadgeAwardService
import com.brifo.server.batch.settlement.DecisionSettlementCalculator
import com.brifo.server.batch.settlement.DecisionSettlementItem
import com.brifo.server.batch.settlement.DecisionSettlementService
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.briefing.repository.BriefingRepository
import com.brifo.server.decision.entity.Decision
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.decision.repository.DecisionRepository
import com.brifo.server.news.entity.ImportanceBadge
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.entity.NewsSource
import com.brifo.server.news.repository.NewsCardRepository
import com.brifo.server.news.repository.NewsRepository
import com.brifo.server.stock.entity.DailyStockPrice
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.repository.DailyStockPriceRepository
import com.brifo.server.stock.repository.UserStockRepository
import com.brifo.server.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.random.Random

/**
 * 게스트 체험 계정이 처음부터 빈 화면을 보지 않도록, 이미 종가가 확정된 과거 날짜의
 * 실제 뉴스카드를 재사용해 완결된 예측(적중/실패)과 결정일기를 즉시 만들어준다.
 * 정산 로직은 실제 배치가 쓰는 DecisionSettlementService를 그대로 재사용해
 * AP 지급·배지·경험치·알림까지 실제 서비스와 동일하게 처리한다.
 *
 * 날짜는 운영 DB에 뉴스카드·종가가 실제로 채워져 있는 것이 확인된 2026-09-14~18로 고정하고,
 * 하루도 비지 않도록 5일 모두에 결정일기를 채운다. 종목은 사용자가 온보딩에서 직접 고른
 * 관심종목(UserStock) 중에서만 고른다 — 관심종목과 무관한 임의 종목(예: 삼성전자)이 섞여
 * 들어가지 않게 하면서도, 하루에 같은 종목이 두 번 배정되는 일은 없다(existsDailyDecision이
 * 강제하는 "사용자당 종목당 하루 1건" 규칙과 자연히 맞는다). 매일 관심종목 전부를 기계적으로
 * 채우면 부자연스러워 보이므로, 하루에 최소 1개는 보장하면서 몇 개·어느 종목이 들어갈지와
 * 담당 사원(루키/프로/탱커)을 날짜·종목마다 무작위로 섞는다.
 *
 * 관심종목에 그 5일치 실제 뉴스카드·종가가 없을 수도 있다(수집 대상이 아니었던 종목 등).
 * 이 경우에도 온보딩이 빈 화면으로 끝나지 않도록 `NewsSource.TEST`로 표시된 안내용
 * 뉴스카드·종가를 즉석에서 만들어 채운다. 결정일기 API는 뉴스카드 본문을 내려주지 않고
 * 종목명·가격·등락률·적중여부만 보여주므로, 이 안내용 데이터로도 화면 표시에는 문제가 없다.
 * 또한 실제 뉴스 피드(`NewsService.getNewsCards`)는 항상 오늘 날짜만 조회하므로, 과거로
 * 고정된 이 안내용 뉴스카드가 실제 사용자에게 노출될 경로는 없다.
 *
 * 결정일기 캘린더(`DiaryQueryRepositoryImpl.findCalendarRows`/`findDayDetailRows`)는
 * `news_cards.display_date`도 `daily_stock_prices.trade_date`도 보지 않고 오직
 * `decisions.created_at`으로만 날짜를 나눈다. `Decision.createdAt`은 `@CreatedDate` +
 * `updatable = false`라 JPA 저장만으로는 과거로 되돌릴 수 없어서, 저장 직후 네이티브 UPDATE로
 * 직접 덮어쓴다(`forceCreatedAtForGuestSeeding`). 이걸 빼먹으면 5일치 데이터가 캘린더에는
 * 전부 온보딩을 실제로 완료한 "오늘" 하루에 몰려 보인다.
 */
@Service
class GuestMockDataSeedingService(
    private val newsRepository: NewsRepository,
    private val newsCardRepository: NewsCardRepository,
    private val dailyStockPriceRepository: DailyStockPriceRepository,
    private val agentRepository: AgentRepository,
    private val userStockRepository: UserStockRepository,
    private val briefingRepository: BriefingRepository,
    private val decisionRepository: DecisionRepository,
    private val apTransactionService: ApTransactionService,
    private val badgeAwardService: BadgeAwardService,
    private val decisionSettlementService: DecisionSettlementService,
    private val calculator: DecisionSettlementCalculator,
) {
    @Transactional
    fun seed(user: User) {
        val agents = agentRepository.findAllByUserId(requireNotNull(user.id))
        if (agents.isEmpty()) return

        val stocks = userStockRepository.findAllByUser(user).map { it.stock }
        if (stocks.isEmpty()) return

        var index = 0
        for (date in FIXED_DATES) {
            // 매일 관심종목 전부를 채우면 기계적으로 반복돼 보이니, 하루에 최소 1개는
            // 보장하면서 몇 개·어느 종목이 들어갈지를 날짜마다 무작위로 섞는다.
            val stocksForDay = stocks.shuffled().take(Random.nextInt(1, stocks.size + 1))
            for ((stockIndex, stock) in stocksForDay.withIndex()) {
                val agent = agents.random()
                val correct = OUTCOMES[index % OUTCOMES.size]
                seedOne(user, agent, stock, date, correct, index, stockIndex)
                index++
            }
        }
    }

    /** 해당 날짜·종목의 결정일기 1건을 만든다. 실제 뉴스카드·종가가 없으면 안내용으로 대신 채운다. */
    private fun seedOne(
        user: User,
        agent: Agent,
        stock: Stock,
        date: LocalDate,
        correct: Boolean,
        seedIndex: Int,
        stockIndexInDay: Int,
    ) {
        val newsCard = findOrCreatePlaceholderNewsCard(stock, date)
        val closingPrice = findOrCreatePlaceholderClosingPrice(stock, date, seedIndex)

        val actualDirection = calculator.actualDirection(closingPrice.changeRate)
        val predictedDirection = if (correct) actualDirection else wrongDirection(actualDirection)

        // 하루에 같은 종목이 아닌 여러 종목이 생길 수 있으니, 캘린더/목록 정렬이 흐트러지지 않도록
        // 같은 날짜 안에서도 종목 순서대로 시각을 조금씩 벌린다.
        val backdatedAt = date.atTime(9, 0).plusMinutes(stockIndexInDay * 5L)

        val briefing = Briefing.create(listOf(newsCard), agent)
        briefing.startAnalysis()
        briefing.complete(
            direction = BriefingDirection.valueOf(predictedDirection.name),
            confidenceRate = CONFIDENCE_RATE,
            contentText = buildContentText(stock, newsCard, predictedDirection),
            oneLiner = "${stock.name} ${directionLabel(predictedDirection)} 전망",
            headline = "${stock.name}, ${directionLabel(predictedDirection)} 흐름 예상",
            summary = "${newsCard.headline} 이슈를 검토했을 때 ${directionLabel(predictedDirection)} 가능성이 높다고 판단했습니다.",
            personalComment = null,
        )
        briefingRepository.save(briefing)
        briefingRepository.forceCreatedAtForGuestSeeding(requireNotNull(briefing.id), backdatedAt)

        val decision =
            decisionRepository.save(
                Decision.create(
                    briefing = briefing,
                    direction = predictedDirection,
                    allocatedAp = ALLOCATED_AP,
                    allocationRatePercent = ALLOCATION_RATE_PERCENT,
                ),
            )
        val decisionId = requireNotNull(decision.id)
        decisionRepository.forceCreatedAtForGuestSeeding(decisionId, backdatedAt)
        val userPublicId = requireNotNull(user.publicId)

        apTransactionService.change(
            userId = userPublicId,
            deltaAp = -ALLOCATED_AP,
            reason = ApTransactionReason.DECISION_ENTRY_FEE,
            target = ApTransactionService.Target(ApTransactionTargetType.DECISION, decisionId),
        )
        badgeAwardService.awardBadge(userPublicId, BadgeCode.B02)

        decisionSettlementService.settle(
            DecisionSettlementItem(
                decisionId = decisionId,
                dailyStockPriceId = requireNotNull(closingPrice.id),
                isCorrect = correct,
            ),
        )
    }

    /** 실제 뉴스카드가 있으면 그걸 쓰고, 없으면 안내용 뉴스카드를 만들어 저장한다. */
    private fun findOrCreatePlaceholderNewsCard(
        stock: Stock,
        date: LocalDate,
    ): NewsCard {
        newsCardRepository.findDailyCards(requireNotNull(stock.publicId), date).firstOrNull()?.let { return it }

        val dedupKey = "guest-mock:${stock.id}:$date"
        val news =
            newsRepository.save(
                News.create(
                    stock = stock,
                    source = NewsSource.TEST,
                    sourceUrl = "https://brifo.internal/guest-mock/${stock.id}/$date",
                    title = "${stock.name} 관련 소식",
                    summary = null,
                    importance = null,
                    dedupKey = dedupKey,
                    publishedAt = date.atTime(9, 0),
                ),
            )
        return newsCardRepository.save(
            NewsCard.create(
                news = news,
                headline = "${stock.name} 관련 소식",
                points = listOf("게스트 체험용으로 채워진 안내 카드입니다."),
                keywords = emptyList(),
                importanceBadge = ImportanceBadge.LOW,
                displayDate = date,
            ),
        )
    }

    /** 실제 종가가 있으면 그걸 쓰고, 없으면 방향 판정 임계값을 넘는 안내용 종가를 만들어 저장한다. */
    private fun findOrCreatePlaceholderClosingPrice(
        stock: Stock,
        date: LocalDate,
        seedIndex: Int,
    ): DailyStockPrice {
        dailyStockPriceRepository.findByStockIdAndTradeDateAndIsClosingTrue(requireNotNull(stock.id), date)?.let {
            return it
        }

        return dailyStockPriceRepository.save(
            DailyStockPrice.createClosing(
                stock = stock,
                tradeDate = date,
                price = PLACEHOLDER_PRICES[seedIndex % PLACEHOLDER_PRICES.size],
                changeRate = PLACEHOLDER_CHANGE_RATES[seedIndex % PLACEHOLDER_CHANGE_RATES.size],
            ),
        )
    }

    private fun buildContentText(
        stock: Stock,
        newsCard: NewsCard,
        direction: DecisionDirection,
    ): String {
        val point = newsCard.points.firstOrNull()
        val pointText = if (point != null) " 핵심 포인트는 '$point'입니다." else ""
        return "${stock.name} 관련 소식 '${newsCard.headline}'을 검토한 결과, " +
            "${directionLabel(direction)} 가능성이 높다고 판단했습니다.$pointText"
    }

    private fun directionLabel(direction: DecisionDirection): String =
        when (direction) {
            DecisionDirection.UP -> "상승"
            DecisionDirection.DOWN -> "하락"
            DecisionDirection.NEUTRAL -> "보합"
        }

    private fun wrongDirection(actual: DecisionDirection): DecisionDirection =
        when (actual) {
            DecisionDirection.UP -> DecisionDirection.DOWN
            DecisionDirection.DOWN -> DecisionDirection.UP
            DecisionDirection.NEUTRAL -> DecisionDirection.UP
        }

    private companion object {
        const val ALLOCATED_AP = 50_000
        const val ALLOCATION_RATE_PERCENT = 5
        const val CONFIDENCE_RATE: Short = 75

        /** 하루도 빠짐없이 결정일기를 채우는 고정 날짜(최신순). */
        val FIXED_DATES =
            listOf(
                LocalDate.of(2026, 9, 18),
                LocalDate.of(2026, 9, 17),
                LocalDate.of(2026, 9, 16),
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 14),
            )

        /** 날짜 인덱스 순으로 돌아가며 적용해 대략 2적중 1실패 비율이 되도록 한다. */
        val OUTCOMES = listOf(true, false, true)

        /**
         * 관심종목에 실제 종가가 없을 때 대신 채우는 안내용 값.
         * 등락률은 방향 판정 임계값(0.5%)을 뚜렷이 넘겨 보합으로 판정되지 않게 한다.
         */
        val PLACEHOLDER_PRICES = listOf(BigDecimal("52000.00"), BigDecimal("135000.00"), BigDecimal("221000.00"))
        val PLACEHOLDER_CHANGE_RATES = listOf(BigDecimal("1.80"), BigDecimal("-1.20"), BigDecimal("2.30"))
    }
}
