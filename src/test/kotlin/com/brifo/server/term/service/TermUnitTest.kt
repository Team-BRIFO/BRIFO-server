package com.brifo.server.term.service

import com.brifo.server.badge.code.BadgeCode
import com.brifo.server.badge.service.BadgeAwardService
import com.brifo.server.term.dto.request.GetMyTermsRequest
import com.brifo.server.term.dto.response.GetMyTermsResponse
import com.brifo.server.term.entity.GlossaryTerm
import com.brifo.server.term.exception.TermNotFoundException
import com.brifo.server.term.repository.GlossaryTermRepository
import com.brifo.server.term.repository.UserLearnedTermRepository
import com.brifo.server.user.entity.User
import com.brifo.server.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.UUID

class TermUnitTest {
    private lateinit var glossaryTermRepository: GlossaryTermRepository
    private lateinit var userLearnedTermRepository: UserLearnedTermRepository
    private lateinit var userRepository: UserRepository
    private lateinit var badgeAwardService: BadgeAwardService
    private lateinit var termService: TermService
    private val userPublicId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        glossaryTermRepository = mock(GlossaryTermRepository::class.java)
        userLearnedTermRepository = mock(UserLearnedTermRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        badgeAwardService = mock(BadgeAwardService::class.java)
        val user = mock(User::class.java)
        `when`(user.id).thenReturn(7L)
        `when`(userRepository.findByPublicId(userPublicId)).thenReturn(user)
        termService = TermService(glossaryTermRepository, userLearnedTermRepository, userRepository, badgeAwardService)
    }

    @Test
    fun `상세 조회는 현재 사용자의 학습 여부를 포함한다`() {
        val publicId = UUID.randomUUID()
        val term = term(id = 31L, publicId = publicId)
        `when`(glossaryTermRepository.findByPublicId(publicId)).thenReturn(term)
        `when`(userLearnedTermRepository.existsByUserIdAndTermId(7L, 31L)).thenReturn(true)

        val response = termService.getTerm(userPublicId = userPublicId, termId = publicId)

        assertEquals(publicId, response.termId)
        assertEquals("PER", response.term)
        assertEquals("주가수익비율", response.definition)
        assertEquals("지표", response.category)
        assertTrue(response.isLearned)
    }

    @Test
    fun `존재하지 않는 용어의 상세 조회와 저장은 TERM_404 예외를 던진다`() {
        val publicId = UUID.randomUUID()
        `when`(glossaryTermRepository.findByPublicId(publicId)).thenReturn(null)

        assertThrows(TermNotFoundException::class.java) {
            termService.getTerm(userPublicId = userPublicId, termId = publicId)
        }
        assertThrows(TermNotFoundException::class.java) {
            termService.saveTerm(userPublicId = userPublicId, termId = publicId)
        }
    }

    @Test
    fun `저장은 공개 용어 ID를 내부 ID로 변환해 멱등 INSERT를 호출한다`() {
        val publicId = UUID.randomUUID()
        val term = term(id = 31L, publicId = publicId)
        `when`(glossaryTermRepository.findByPublicId(publicId)).thenReturn(term)

        termService.saveTerm(userPublicId = userPublicId, termId = publicId)

        verify(userLearnedTermRepository).insertIfAbsent(userId = 7L, termId = 31L)
    }

    @Test
    fun `학습한 용어가 10개 이상이면 공부하는 사장 뱃지를 지급한다`() {
        val publicId = UUID.randomUUID()
        val term = term(id = 31L, publicId = publicId)
        `when`(glossaryTermRepository.findByPublicId(publicId)).thenReturn(term)
        `when`(userLearnedTermRepository.countByUserId(7L)).thenReturn(10L)

        termService.saveTerm(userPublicId = userPublicId, termId = publicId)

        verify(badgeAwardService).awardBadge(userPublicId, BadgeCode.B11)
    }

    @Test
    fun `빈 목록은 전체 개수 0과 다음 페이지 없음으로 반환한다`() {
        val request = GetMyTermsRequest(size = 20)
        `when`(userLearnedTermRepository.findPageByUserId(7L, null, 21)).thenReturn(emptyList())
        `when`(userLearnedTermRepository.countByUserId(7L)).thenReturn(0L)

        val response = termService.getMyTerms(userPublicId = userPublicId, request = request)

        assertEquals(0, response.learnedTermCount)
        assertTrue(response.page.items.isEmpty())
        assertFalse(response.page.hasNext)
        assertNull(response.page.nextCursor)
    }

    @Test
    fun `size개 이하이면 항목을 모두 반환하고 다음 커서는 없다`() {
        val records = (1..2).map { learnedTerm(uuid(it)) }
        `when`(userLearnedTermRepository.findPageByUserId(7L, null, 3)).thenReturn(records)
        `when`(userLearnedTermRepository.countByUserId(7L)).thenReturn(5L)

        val response = termService.getMyTerms(userPublicId, GetMyTermsRequest(size = 2))

        assertEquals(5, response.learnedTermCount)
        assertEquals(2, response.page.items.size)
        assertFalse(response.page.hasNext)
        assertNull(response.page.nextCursor)
    }

    @Test
    fun `커서 이후 항목을 하나 더 조회하면 size개와 다음 커서를 반환한다`() {
        val cursor = UUID.randomUUID()
        val records = (3 downTo 1).map { learnedTerm(uuid(it)) }
        `when`(userLearnedTermRepository.findPageByUserId(7L, cursor, 3)).thenReturn(records)
        `when`(userLearnedTermRepository.countByUserId(7L)).thenReturn(3L)

        val response = termService.getMyTerms(userPublicId, GetMyTermsRequest(cursor = cursor, size = 2))

        assertEquals(2, response.page.items.size)
        assertTrue(response.page.hasNext)
        assertEquals(uuid(2), response.page.nextCursor)
        verify(userLearnedTermRepository).findPageByUserId(7L, cursor, 3)
    }

    private fun term(
        id: Long,
        publicId: UUID,
    ): GlossaryTerm =
        mock(GlossaryTerm::class.java).also {
            `when`(it.id).thenReturn(id)
            `when`(it.publicId).thenReturn(publicId)
            `when`(it.term).thenReturn("PER")
            `when`(it.definition).thenReturn("주가수익비율")
            `when`(it.category).thenReturn("지표")
        }

    private fun learnedTerm(publicId: UUID): GetMyTermsResponse.Item =
        GetMyTermsResponse.Item(
            learnedTermId = publicId,
            termId = UUID.randomUUID(),
            term = "PER",
            definition = "주가수익비율",
            category = "지표",
            learnedAt = LocalDateTime.of(2026, 7, 1, 10, 0),
        )

    private fun uuid(value: Int): UUID =
        UUID.fromString("00000000-0000-0000-0000-${value.toString().padStart(12, '0')}")
}
