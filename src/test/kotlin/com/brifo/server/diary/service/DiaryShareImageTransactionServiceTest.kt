package com.brifo.server.diary.service

import com.brifo.server.diary.entity.DiaryEntry
import com.brifo.server.diary.repository.DiaryEntryRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertFalse

class DiaryShareImageTransactionServiceTest {
    private val repository = mock(DiaryEntryRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-11T01:30:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = DiaryShareImageTransactionService(repository, clock)

    @Test
    fun `공유 이미지 생성 시각은 애플리케이션 Clock을 사용한다`() {
        val userId = UUID.randomUUID()
        val diaryId = UUID.randomUUID()
        val diary = mock(DiaryEntry::class.java)
        val imageKey = "$diaryId.png"
        `when`(repository.findOwnedByPublicIdForUpdate(userId, diaryId)).thenReturn(diary)
        `when`(diary.shareImageUrl).thenReturn(null)

        val result = service.createIfAbsent(userId, diaryId) { imageKey }

        assertFalse(result.reused)
        verify(diary).attachShareImage(imageKey, LocalDateTime.ofInstant(clock.instant(), clock.zone))
    }

    @Test
    fun `기존 공개 URL은 새 객체 key로 교체한다`() {
        val userId = UUID.randomUUID()
        val diaryId = UUID.randomUUID()
        val diary = mock(DiaryEntry::class.java)
        val imageKey = "$diaryId.png"
        `when`(repository.findOwnedByPublicIdForUpdate(userId, diaryId)).thenReturn(diary)
        `when`(diary.shareImageUrl).thenReturn("https://s3.example.com/$imageKey")

        val result = service.createIfAbsent(userId, diaryId) { imageKey }

        assertFalse(result.reused)
        verify(diary).attachShareImage(imageKey, LocalDateTime.ofInstant(clock.instant(), clock.zone))
    }
}
