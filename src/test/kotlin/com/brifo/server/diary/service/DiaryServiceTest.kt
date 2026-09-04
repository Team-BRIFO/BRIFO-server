package com.brifo.server.diary.service

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.agent.repository.AgentRepository
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.decision.entity.DecisionDirection
import com.brifo.server.diary.dto.request.GetDiariesRequest
import com.brifo.server.diary.dto.request.GetDiaryCalendarRequest
import com.brifo.server.diary.exception.DiaryNotFoundException
import com.brifo.server.diary.repository.DiaryCalendarRow
import com.brifo.server.diary.repository.DiaryEntryRepository
import com.brifo.server.diary.repository.DiaryDetailRow
import com.brifo.server.diary.repository.DiaryListRow
import com.brifo.server.diary.share.ShareImageStorage
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiaryServiceTest {
    private val repository = mock(DiaryEntryRepository::class.java)
    private val shareImageStorage = mock(ShareImageStorage::class.java)
    private val service = DiaryService(
        repository,
        mock(AgentRepository::class.java),
        mock(DiaryStatsCalculator::class.java),
        shareImageStorage,
    )
    private val userId = UUID.randomUUID()

    @Test
    fun `다음 행이 있으면 반환한 마지막 일기를 커서로 사용한다`() {
        val rows = (1..3).map(::listRow)
        `when`(repository.findDiaryPage(userId, null, 3)).thenReturn(rows)

        val page = service.getDiaries(userId, GetDiariesRequest(size = 2)).page

        assertEquals(rows.take(2).map { it.diaryId }, page.items.map { it.diaryId })
        assertEquals(72_500L, page.items.first().stock.price)
        assertEquals(BigDecimal("2.6"), page.items.first().stock.changeRate)
        assertEquals(LocalDate.of(2026, 7, 20), page.items.first().stock.tradeDate)
        assertEquals("https://example.com/stocks/1.png", page.items.first().stock.logoUrl)
        assertTrue(page.hasNext)
        assertEquals(rows[1].diaryId, page.nextCursor)
    }

    @Test
    fun `다음 행이 없으면 다음 커서를 노출하지 않는다`() {
        val rows = listOf(listRow(1))
        `when`(repository.findDiaryPage(userId, null, 3)).thenReturn(rows)

        val page = service.getDiaries(userId, GetDiariesRequest(size = 2)).page

        assertFalse(page.hasNext)
        assertNull(page.nextCursor)
    }

    @Test
    fun `소유한 정산 일기가 없으면 상세 조회에 실패한다`() {
        val diaryId = UUID.randomUUID()
        `when`(repository.findDiaryDetail(userId, diaryId)).thenReturn(null)

        assertFailsWith<DiaryNotFoundException> {
            service.getDiaryDetail(userId, diaryId)
        }
    }

    @Test
    fun `상세 등락률은 소수점 한 자리로 반올림한다`() {
        val diaryId = UUID.randomUUID()
        val imageKey = "$diaryId.png"
        val imageUrl = "https://s3.example.com/$imageKey?signature=test"
        `when`(repository.findDiaryDetail(userId, diaryId)).thenReturn(
            DiaryDetailRow(
                diaryId = diaryId,
                shareImageUrl = imageKey,
                stockId = UUID.randomUUID(),
                stockName = "삼성전자",
                changeRate = BigDecimal("2.55"),
                tradeDate = LocalDate.of(2026, 8, 10),
                apDelta = 80,
                agentId = UUID.randomUUID(),
                agentType = AgentType.ROOKIE,
                agentNickname = "루키",
                briefingId = UUID.randomUUID(),
                briefingDirection = BriefingDirection.UP,
                briefingConfidenceRate = 72,
                isCorrect = true,
                confidenceLevel = 4,
            ),
        )
        `when`(shareImageStorage.createDownloadUrl(imageKey)).thenReturn(imageUrl)

        val detail = service.getDiaryDetail(userId, diaryId)

        assertEquals(BigDecimal("2.6"), detail.stock.changeRate)
        assertEquals(imageUrl, detail.shareImageUrl)
    }

    @Test
    fun `상세 조회에서 기존 공개 URL은 presign하지 않는다`() {
        val diaryId = UUID.randomUUID()
        `when`(repository.findDiaryDetail(userId, diaryId)).thenReturn(
            detailRow(diaryId, "https://s3.example.com/$diaryId.png"),
        )

        val detail = service.getDiaryDetail(userId, diaryId)

        assertNull(detail.shareImageUrl)
        verifyNoInteractions(shareImageStorage)
    }

    @Test
    fun `캘린더는 같은 날짜의 결정 결과를 합친다`() {
        val firstDay = LocalDate.of(2026, 7, 1)
        `when`(
            repository.findCalendarRows(
                userId,
                firstDay.atStartOfDay(),
                firstDay.plusMonths(1).atStartOfDay(),
            ),
        ).thenReturn(
            listOf(
                calendarRow(2, DecisionDirection.UP, true),
                calendarRow(2, DecisionDirection.DOWN, false),
            ),
        )

        val result = service.getDiaryCalendar(userId, GetDiaryCalendarRequest(2026, 7))

        assertEquals(1, result.days.size)
        assertTrue(result.days.single().outcome.decisionWin)
        assertTrue(result.days.single().outcome.decisionLoss)
        assertFalse(result.days.single().outcome.neutralHit)
        assertEquals(50, result.accuracyRate)
    }

    private fun listRow(index: Int) =
        DiaryListRow(
            diaryId = UUID.randomUUID(),
            stockId = UUID.randomUUID(),
            stockName = "종목 $index",
            price = BigDecimal("72500.00"),
            changeRate = BigDecimal("2.55"),
            tradeDate = LocalDate.of(2026, 7, 20),
            logoUrl = "https://example.com/stocks/$index.png",
            direction = DecisionDirection.UP,
            apDelta = 10,
            isCorrect = true,
        )

    private fun calendarRow(
        day: Int,
        direction: DecisionDirection,
        isCorrect: Boolean,
    ) = DiaryCalendarRow(LocalDateTime.of(2026, 7, day, 10, 0), direction, isCorrect)

    private fun detailRow(
        diaryId: UUID,
        shareImageUrl: String?,
    ) = DiaryDetailRow(
        diaryId = diaryId,
        shareImageUrl = shareImageUrl,
        stockId = UUID.randomUUID(),
        stockName = "삼성전자",
        changeRate = BigDecimal("2.55"),
        tradeDate = LocalDate.of(2026, 8, 10),
        apDelta = 80,
        agentId = UUID.randomUUID(),
        agentType = AgentType.ROOKIE,
        agentNickname = "루키",
        briefingId = UUID.randomUUID(),
        briefingDirection = BriefingDirection.UP,
        briefingConfidenceRate = 72,
        isCorrect = true,
        confidenceLevel = 4,
    )
}
