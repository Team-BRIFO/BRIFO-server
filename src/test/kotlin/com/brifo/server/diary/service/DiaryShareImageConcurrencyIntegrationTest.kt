package com.brifo.server.diary.service

import com.brifo.server.ServerTestConfiguration
import com.brifo.server.batch.anyKotlin
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.briefing.support.BriefingDatabaseFixture
import com.brifo.server.decision.entity.Decision
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.decision.entity.DecisionResult
import com.brifo.server.diary.dto.response.CreateDiaryShareImageResponse
import com.brifo.server.diary.entity.DiaryEntry
import com.brifo.server.diary.repository.DiaryEntryRepository
import com.brifo.server.diary.share.DiaryShareImageRenderer
import com.brifo.server.diary.share.ShareImageFile
import com.brifo.server.diary.share.ShareImageStorage
import com.brifo.server.stock.entity.DailyStockPrice
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Import(ServerTestConfiguration::class)
@ActiveProfiles("test")
@SpringBootTest
class DiaryShareImageConcurrencyIntegrationTest {
    @Autowired private lateinit var service: DiaryShareImageService
    @Autowired private lateinit var repository: DiaryEntryRepository
    @Autowired private lateinit var entityManager: EntityManager
    @Autowired private lateinit var transactionTemplate: TransactionTemplate

    @MockitoBean
    private lateinit var renderer: DiaryShareImageRenderer

    @MockitoBean
    private lateinit var storage: ShareImageStorage

    @Test
    fun `동일 일기의 동시 요청은 이미지를 한 번만 업로드한다`() {
        val fixture = transactionTemplate.execute { createDiaryFixture() }!!
        val file = ShareImageFile(byteArrayOf(1, 2, 3), "image/png", "png")
        val imageUrl = "https://cdn.brifo.app/diary-share-images/${fixture.diaryPublicId}.png"
        val uploadStarted = CountDownLatch(1)
        val releaseUpload = CountDownLatch(1)
        `when`(renderer.render(anyKotlin())).thenReturn(file)
        `when`(storage.store(fixture.diaryPublicId, file)).thenAnswer {
            uploadStarted.countDown()
            check(releaseUpload.await(5, TimeUnit.SECONDS)) { "concurrent request did not start" }
            imageUrl
        }

        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        val futures =
            (1..2).map {
                executor.submit<CreateDiaryShareImageResponse> {
                    ready.countDown()
                    start.await()
                    service.create(fixture.userPublicId, fixture.diaryPublicId)
                }
            }

        check(ready.await(5, TimeUnit.SECONDS)) { "requests were not ready" }
        start.countDown()
        check(uploadStarted.await(5, TimeUnit.SECONDS)) { "upload did not start" }
        Thread.sleep(200)
        releaseUpload.countDown()
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()

        assertEquals(listOf(false, true), results.map { it.reused }.sorted())
        assertTrue(results.all { it.shareImageUrl == imageUrl })
        verify(renderer, times(1)).render(anyKotlin())
        verify(storage, times(1)).store(fixture.diaryPublicId, file)
        val diary =
            transactionTemplate.execute {
                repository.findOwnedByPublicIdForUpdate(fixture.userPublicId, fixture.diaryPublicId)
            }!!
        assertEquals(imageUrl, diary.shareImageUrl)
        assertNotNull(diary.shareImageCreatedAt)
    }

    private fun createDiaryFixture(): Fixture {
        val scenario = BriefingDatabaseFixture(entityManager).requestScenario(DATE, agentCount = 1)
        val briefing =
            Briefing.create(scenario.cards, scenario.agents.single()).also {
                it.startAnalysis()
                it.complete(BriefingDirection.UP, 72, "분석", "한줄", "제목", "요약", null)
                entityManager.persist(it)
            }
        val decision = Decision.create(briefing, DecisionDirection.UP, 4).also(entityManager::persist)
        val price =
            DailyStockPrice.create(
                stock = scenario.stock,
                tradeDate = DATE,
                price = BigDecimal("70000.00"),
                changeRate = BigDecimal("2.55"),
            ).also(entityManager::persist)
        entityManager.persist(DecisionResult.create(decision, price, true))
        val diary = DiaryEntry.create(decision).also(entityManager::persist)
        entityManager.flush()
        entityManager.refresh(diary)
        return Fixture(scenario.user.publicId!!, diary.publicId!!)
    }

    private data class Fixture(
        val userPublicId: UUID,
        val diaryPublicId: UUID,
    )

    companion object {
        private val DATE = LocalDate.of(2026, 8, 11)
    }
}
