package com.brifo.server.diary.service

import com.brifo.server.agent.entity.AgentType
import com.brifo.server.briefing.entity.BriefingDirection
import com.brifo.server.diary.exception.DiaryNotFoundException
import com.brifo.server.diary.exception.DiaryShareImageGenerationFailedException
import com.brifo.server.diary.repository.DiaryDetailRow
import com.brifo.server.diary.repository.DiaryEntryRepository
import com.brifo.server.diary.share.DiaryShareImageModel
import com.brifo.server.diary.share.DiaryShareImageRenderer
import com.brifo.server.diary.share.ShareImageFile
import com.brifo.server.diary.share.ShareImageStorage
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiaryShareImageServiceTest {
    private val repository = mock(DiaryEntryRepository::class.java)
    private val renderer = mock(DiaryShareImageRenderer::class.java)
    private val storage = mock(ShareImageStorage::class.java)
    private var transactionResult: AttachedShareImage? = null
    private val transactionService =
        mock(DiaryShareImageTransactionService::class.java) { invocation ->
            if (invocation.method.name == "createIfAbsent") {
                transactionResult
                    ?: AttachedShareImage(invocation.getArgument<() -> String>(2).invoke(), reused = false)
            } else {
                Answers.RETURNS_DEFAULTS.answer(invocation)
            }
        }
    private val service = DiaryShareImageService(repository, renderer, storage, transactionService)
    private val userId = UUID.randomUUID()
    private val diaryId = UUID.randomUUID()

    @Test
    fun `기존 공유 이미지가 있으면 생성과 저장 없이 재사용한다`() {
        val existingUrl = "https://cdn.brifo.app/existing.png"
        `when`(repository.findDiaryDetail(userId, diaryId)).thenReturn(row(existingUrl))

        val result = service.create(userId, diaryId)

        assertEquals(existingUrl, result.shareImageUrl)
        assertTrue(result.reused)
        verifyNoInteractions(renderer, storage, transactionService)
    }

    @Test
    fun `공유 이미지를 생성하고 저장한 URL을 일기에 연결한다`() {
        val file = ShareImageFile(byteArrayOf(1, 2, 3), "image/png", "png")
        val generatedUrl = "https://cdn.brifo.app/$diaryId.png"
        `when`(repository.findDiaryDetail(userId, diaryId)).thenReturn(row())
        `when`(renderer.render(model())).thenReturn(file)
        `when`(storage.store(diaryId, file)).thenReturn(generatedUrl)

        val result = service.create(userId, diaryId)

        assertEquals(generatedUrl, result.shareImageUrl)
        assertFalse(result.reused)
    }

    @Test
    fun `동시 요청에서 먼저 저장된 이미지가 있으면 그 URL을 재사용한다`() {
        val existingUrl = "https://cdn.brifo.app/existing.png"
        `when`(repository.findDiaryDetail(userId, diaryId)).thenReturn(row())
        transactionResult = AttachedShareImage(existingUrl, reused = true)

        val result = service.create(userId, diaryId)

        assertEquals(existingUrl, result.shareImageUrl)
        assertTrue(result.reused)
        verifyNoInteractions(renderer, storage)
    }

    @Test
    fun `소유한 결정일기가 없으면 생성하지 않는다`() {
        `when`(repository.findDiaryDetail(userId, diaryId)).thenReturn(null)

        assertFailsWith<DiaryNotFoundException> { service.create(userId, diaryId) }
        verifyNoInteractions(renderer, storage, transactionService)
    }

    @Test
    fun `렌더링이나 저장에 실패하면 공유 이미지 생성 실패로 변환한다`() {
        `when`(repository.findDiaryDetail(userId, diaryId)).thenReturn(row())
        `when`(renderer.render(model())).thenThrow(IllegalStateException("renderer failed"))

        assertFailsWith<DiaryShareImageGenerationFailedException> {
            service.create(userId, diaryId)
        }
        verifyNoInteractions(storage)
    }

    @Test
    fun `저장소 업로드에 실패하면 공유 이미지 생성 실패로 변환한다`() {
        val file = ShareImageFile(byteArrayOf(1, 2, 3), "image/png", "png")
        `when`(repository.findDiaryDetail(userId, diaryId)).thenReturn(row())
        `when`(renderer.render(model())).thenReturn(file)
        `when`(storage.store(diaryId, file)).thenThrow(IllegalStateException("storage failed"))

        assertFailsWith<DiaryShareImageGenerationFailedException> {
            service.create(userId, diaryId)
        }
    }

    private fun row(shareImageUrl: String? = null) =
        DiaryDetailRow(
            diaryId = diaryId,
            shareImageUrl = shareImageUrl,
            stockId = UUID.randomUUID(),
            stockName = "삼성전자",
            changeRate = BigDecimal("2.5"),
            agentId = UUID.randomUUID(),
            agentType = AgentType.ROOKIE,
            agentNickname = "루키",
            briefingId = UUID.randomUUID(),
            briefingDirection = BriefingDirection.UP,
            briefingConfidenceRate = 72,
            isCorrect = true,
            confidenceLevel = 4,
        )

    private fun model() =
        DiaryShareImageModel(
            diaryId = diaryId,
            stockName = "삼성전자",
            changeRate = BigDecimal("2.5"),
            agentType = AgentType.ROOKIE,
            agentNickname = "루키",
            briefingDirection = BriefingDirection.UP,
            briefingConfidenceRate = 72,
            isCorrect = true,
            decisionConfidenceLevel = 4,
        )
}
