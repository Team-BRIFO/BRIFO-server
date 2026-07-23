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
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
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
    private val service = DiaryService(
        repository,
        mock(AgentRepository::class.java),
        mock(DiaryStatsCalculator::class.java),
    )
    private val userId = UUID.randomUUID()

    @Test
    fun `다음 행이 있으면 반환한 마지막 일기를 커서로 사용한다`() {
        val rows = (1..3).map(::listRow)
        `when`(repository.findDiaryPage(userId, null, 3)).thenReturn(rows)

        val page = service.getDiaries(userId, GetDiariesRequest(size = 2)).page

        assertEquals(rows.take(2).map { it.diaryId }, page.items.map { it.diaryId })
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
    fun `상세 등락률은 정수로 반올림한다`() {
        val diaryId = UUID.randomUUID()
        `when`(repository.findDiaryDetail(userId, diaryId)).thenReturn(
            DiaryDetailRow(
                diaryId = diaryId,
                shareImageUrl = null,
                stockId = UUID.randomUUID(),
                stockName = "삼성전자",
                changeRate = BigDecimal("2.55"),
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

        val detail = service.getDiaryDetail(userId, diaryId)

        assertEquals(3, detail.stock.changeRate)
    }

    @Test
    fun `캘린더는 같은 날짜의 결정 방향을 합친다`() {
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
        assertTrue(result.days.single().direction.up)
        assertTrue(result.days.single().direction.down)
        assertFalse(result.days.single().direction.neutral)
        assertEquals(50, result.accuracyRate)
    }

    private fun listRow(index: Int) =
        DiaryListRow(
            diaryId = UUID.randomUUID(),
            stockId = UUID.randomUUID(),
            stockName = "종목 $index",
            direction = DecisionDirection.UP,
            apDelta = 10,
            isCorrect = true,
        )

    private fun calendarRow(
        day: Int,
        direction: DecisionDirection,
        isCorrect: Boolean,
    ) = DiaryCalendarRow(LocalDateTime.of(2026, 7, day, 10, 0), direction, isCorrect)
}
