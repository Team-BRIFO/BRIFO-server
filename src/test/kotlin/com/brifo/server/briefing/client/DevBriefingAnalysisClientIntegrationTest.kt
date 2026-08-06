package com.brifo.server.briefing.client

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.support.BriefingDatabaseFixture
import com.brifo.server.news.entity.ImportanceBadge
import com.brifo.server.news.entity.News
import com.brifo.server.news.entity.NewsCard
import com.brifo.server.news.entity.NewsSource
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Import(TestcontainersConfiguration::class)
@ActiveProfiles("dev")
@SpringBootTest
@Transactional
class DevBriefingAnalysisClientIntegrationTest @Autowired constructor(
    private val briefingAnalysisClient: BriefingAnalysisClient,
    private val entityManager: EntityManager,
    private val objectMapper: ObjectMapper,
) {
    @Test
    fun `실제 뉴스 내용으로 브리핑을 생성한다`() {
        // 사용자, 종목, 사원은 기존 fixture로 만든다.
        val scenario =
            BriefingDatabaseFixture(entityManager).requestScenario(
                displayDate = LocalDate.now(),
                agentCount = 3,
            )

        // AI 요청의 레벨 범위를 1-3으로 맞춘다.
        scenario.agents.forEachIndexed { index, agent ->
            // 저장된 사원의 내부 ID를 확인한다.
            val agentId = assertNotNull(agent.id)

            entityManager
                .createQuery("update Agent a set a.level = :level where a.id = :id")
                .setParameter("level", index + 1)
                .setParameter("id", agentId)
                .executeUpdate()
            entityManager.refresh(agent)
        }

        // 실제 뉴스처럼 구체적인 내용을 입력한다.
        val news =
            News.create(
                stock = scenario.stock,
                source = NewsSource.NAVER,
                sourceUrl = "https://news.example.com/samsung-q3",
                title = "삼성전자, 3분기 영업이익 전년 대비 40% 증가",
                summary = "반도체 사업 회복과 메모리 가격 반등으로 실적이 개선됐다.",
                importance = null,
                dedupKey = "samsung-q3-test",
                publishedAt = LocalDateTime.now(),
            ).also(entityManager::persist)

        val newsCard =
            NewsCard.create(
                news = news,
                headline = "삼성전자, 3분기 반도체 실적 시장 예상치 상회",
                points =
                    listOf(
                        "메모리 반도체 가격 상승세가 실적 개선을 견인",
                        "AI 서버용 반도체 수요 증가로 4분기 전망도 긍정적",
                        "외국인 투자자의 순매수 전환 가능성이 커지고 있음",
                    ),
                keywords = listOf("삼성전자", "반도체", "실적"),
                importanceBadge = ImportanceBadge.HOT,
                displayDate = LocalDate.now(),
            ).also(entityManager::persist)

        // AI 응답과 연결할 브리핑을 만든다.
        val briefings =
            scenario.agents.map { agent ->
                Briefing.create(
                    newsCards = listOf(newsCard),
                    agent = agent,
                ).also(entityManager::persist)
            }

        entityManager.flush()

        // 저장 후 생성된 사용자와 카드뉴스 ID를 확인한다.
        val userId = assertNotNull(scenario.user.publicId)
        val newsCardId = assertNotNull(newsCard.publicId)

        // 요청 대상 브리핑과 사원 ID를 만든다.
        val targets =
            briefings.map { briefing ->
                val briefingId = assertNotNull(briefing.publicId)
                val agentId = assertNotNull(briefing.agent.publicId)

                BriefingAnalysisClient.Target(
                    briefingId = briefingId,
                    agentId = agentId,
                )
            }

        // 배치에서 사용할 기존 브리핑 생성 메서드를 호출한다.
        val result =
            briefingAnalysisClient.createBriefings(
                BriefingAnalysisClient.Request(
                    userId = userId,
                    newsCardIds = listOf(newsCardId),
                    targets = targets,
                    recentDecisionIds = emptyList(),
                ),
            )

        // AI가 생성한 내용을 확인한다.
        println(
            objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(result),
        )

        // 요청한 수만큼 브리핑이 생성됐는지 확인한다.
        assertEquals(targets.size, result.briefings.size)

        val expectedTargets =
            targets
                .map { target -> target.briefingId to target.agentId }
                .toSet()

        val actualTargets =
            result.briefings
                .map { briefing -> briefing.briefingId to briefing.agentId }
                .toSet()

        // 요청과 응답의 브리핑 및 사원 ID가 일치하는지 확인한다.
        assertEquals(expectedTargets, actualTargets)

        // AI 응답의 필수 내용이 비어 있지 않은지 확인한다.
        result.briefings.forEach { briefing ->
            assertTrue(briefing.headline.isNotBlank())
            assertTrue(briefing.summary.isNotBlank())
            assertTrue(briefing.commonAnalysis.isNotBlank())
            assertTrue(briefing.closingComment.isNotBlank())
        }
    }
}
