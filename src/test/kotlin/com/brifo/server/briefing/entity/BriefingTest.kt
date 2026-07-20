package com.brifo.server.briefing.entity

import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.briefing.entity.BriefingStatus
import com.brifo.server.news.entity.ImportanceBadge
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.entity.NewsSource
import com.brifo.server.stock.entity.Stock
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BriefingTest {
    @Test
    fun `같은 종목과 노출일의 카드뉴스로 브리핑을 생성한다`() {
        val stock = Stock.create("005930", "삼성전자", "반도체")
        val displayDate = LocalDate.of(2026, 7, 18)
        val cards = listOf(
            newsCard(stock, "첫 번째 뉴스", displayDate),
            newsCard(stock, "두 번째 뉴스", displayDate),
        )

        val briefing = Briefing.create(cards, agent())

        assertEquals(cards, briefing.newsCards)
        assertEquals(BriefingStatus.PENDING, briefing.status)
    }

    @Test
    fun `서로 다른 종목의 카드뉴스를 연결할 수 없다`() {
        val displayDate = LocalDate.of(2026, 7, 18)
        val cards = listOf(
            newsCard(Stock.create("005930", "삼성전자", "반도체"), "첫 번째 뉴스", displayDate),
            newsCard(Stock.create("000660", "SK하이닉스", "반도체"), "두 번째 뉴스", displayDate),
        )

        assertFailsWith<IllegalArgumentException> { Briefing.create(cards, agent()) }
    }

    @Test
    fun `브리핑은 분석 중 상태에서만 완료할 수 있다`() {
        val stock = Stock.create("005930", "삼성전자", "반도체")
        val displayDate = LocalDate.of(2026, 7, 18)
        val briefing = Briefing.create(
            listOf(
                newsCard(stock, "첫 번째 뉴스", displayDate),
                newsCard(stock, "두 번째 뉴스", displayDate),
            ),
            agent(),
        )

        assertFailsWith<IllegalArgumentException> {
            briefing.complete(
                direction = BriefingDirection.UP,
                confidenceRate = 80,
                contentText = "분석 내용",
                oneLiner = "상승 가능성이 높습니다.",
                headline = "분석 헤드라인",
                summary = "분석 요약",
                personalComment = null,
            )
        }

        briefing.startAnalysis()
        briefing.complete(
            direction = BriefingDirection.UP,
            confidenceRate = 80,
            contentText = "분석 내용",
            oneLiner = "상승 가능성이 높습니다.",
            headline = "분석 헤드라인",
            summary = "분석 요약",
            personalComment = "개인화 코멘트",
        )

        assertEquals(BriefingStatus.COMPLETED, briefing.status)
        assertEquals(BriefingDirection.UP, briefing.direction)
    }

    @Test
    fun `실패한 브리핑은 같은 엔티티를 재사용해 대기 상태가 된다`() {
        val stock = Stock.create("005930", "삼성전자", "반도체")
        val displayDate = LocalDate.of(2026, 7, 18)
        val retryCards = listOf(
            newsCard(stock, "재시도 첫 번째 뉴스", displayDate),
            newsCard(stock, "재시도 두 번째 뉴스", displayDate),
        )
        val briefing = Briefing.create(
            listOf(
                newsCard(stock, "최초 첫 번째 뉴스", displayDate),
                newsCard(stock, "최초 두 번째 뉴스", displayDate),
            ),
            agent(),
        )

        briefing.startAnalysis()
        briefing.fail()
        briefing.retry(retryCards)

        assertEquals(BriefingStatus.PENDING, briefing.status)
        assertEquals(retryCards, briefing.newsCards)
    }

    private fun agent(): Agent {
        val user = User.create(
            provider = OAuthProvider.KAKAO,
            socialId = "social-id",
            email = "user@example.com",
        )
        return Agent.create(
            user = user,
            agentType = AgentType.ROOKIE,
            modelName = "model",
            nickname = "루키",
            description = "설명",
            dailySalary = 10,
        )
    }

    private fun newsCard(
        stock: Stock,
        title: String,
        displayDate: LocalDate,
    ): NewsCard {
        val news = News.create(
            stock = stock,
            source = NewsSource.NAVER,
            sourceUrl = "https://example.com/$title",
            title = title,
            summary = null,
            importance = null,
            dedupKey = title,
            publishedAt = LocalDateTime.of(2026, 7, 18, 8, 0),
        )
        return NewsCard.create(
            news = news,
            headline = title,
            points = listOf("핵심 내용"),
            keywords = listOf("반도체"),
            importanceBadge = ImportanceBadge.HOT,
            displayDate = displayDate,
        )
    }
}
