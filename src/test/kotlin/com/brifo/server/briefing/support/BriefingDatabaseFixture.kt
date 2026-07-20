package com.brifo.server.briefing.support

import com.brifo.server.agent.entity.Agent
import com.brifo.server.agent.entity.AgentType
import com.brifo.server.news.entity.ImportanceBadge
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.entity.NewsSource
import com.brifo.server.stock.entity.Stock
import com.brifo.server.stock.entity.UserStock
import com.brifo.server.user.entity.OAuthProvider
import com.brifo.server.user.entity.User
import jakarta.persistence.EntityManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class BriefingDatabaseFixture(
    private val entityManager: EntityManager,
) {
    fun requestScenario(
        displayDate: LocalDate,
        cardCount: Int = 2,
        agentCount: Int = 3,
        interested: Boolean = true,
        balanceAp: Int = 100,
    ): RequestScenario {
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        val user = User.create(OAuthProvider.KAKAO, "social-$suffix", "$suffix@example.com").also {
            it.refundAp(balanceAp)
            entityManager.persist(it)
        }
        val stock = Stock.create(suffix, "종목-$suffix", "테스트").also(entityManager::persist)
        if (interested) {
            entityManager.persist(UserStock.create(user, stock))
        }
        val cards = (1..cardCount).map { index ->
            val news = News.create(
                stock = stock,
                source = NewsSource.NAVER,
                sourceUrl = "https://example.com/$suffix/$index",
                title = "뉴스-$suffix-$index",
                summary = null,
                importance = null,
                dedupKey = "dedup-$suffix-$index",
                publishedAt = LocalDateTime.of(displayDate, java.time.LocalTime.of(8, 0)),
            ).also(entityManager::persist)
            NewsCard.create(
                news = news,
                headline = "헤드라인-$index",
                points = listOf("핵심"),
                keywords = listOf("키워드"),
                importanceBadge = ImportanceBadge.HOT,
                displayDate = displayDate,
            ).also(entityManager::persist)
        }
        val agents = (1..agentCount).map { index ->
            Agent.create(
                user = user,
                agentType = AgentType.entries[index - 1],
                modelName = "model-$index",
                nickname = "사원-$index",
                description = "설명-$index",
                dailySalary = index * 10,
            ).also(entityManager::persist)
        }
        entityManager.flush()
        return RequestScenario(user, stock, cards, agents)
    }

    data class RequestScenario(
        val user: User,
        val stock: Stock,
        val cards: List<NewsCard>,
        val agents: List<Agent>,
    )
}
