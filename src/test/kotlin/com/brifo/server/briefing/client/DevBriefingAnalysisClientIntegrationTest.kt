package com.brifo.server.briefing.client

import com.brifo.server.TestcontainersConfiguration
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.support.BriefingDatabaseFixture
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
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
    fun `AI 서버에 브리핑을 요청하고 응답 JSON을 출력한다`() {
        // AI 요청에 필요한 뉴스 카드와 사원을 만든다.
        val scenario =
            BriefingDatabaseFixture(entityManager).requestScenario(
                displayDate = LocalDate.now(),
                agentCount = 1,
            )

        // AI 응답과 연결할 브리핑 ID를 만든다.
        val briefing =
            Briefing.create(
                newsCards = scenario.cards,
                agent = scenario.agents.single(),
            )
        entityManager.persist(briefing)
        entityManager.flush()

        // 기존 createBriefings 메서드로 실제 AI 서버를 호출한다.
        val response =
            briefingAnalysisClient.createBriefings(
                BriefingAnalysisClient.Request(
                    userId = scenario.user.publicId!!,
                    newsCardIds = scenario.cards.map { it.publicId!! },
                    targets =
                        listOf(
                            BriefingAnalysisClient.Target(
                                briefingId = briefing.publicId!!,
                                agentId = scenario.agents.single().publicId!!,
                            ),
                        ),
                    recentDecisionIds = emptyList(),
                ),
            )

        // AI 응답을 JSON으로 출력한다.
        println(
            objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(response),
        )

        // 응답에 브리핑이 있는지 확인한다.
        assertTrue(response.briefings.isNotEmpty())
    }
}
