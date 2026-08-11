package com.brifo.server.diary.service

import com.brifo.server.diary.dto.response.CreateDiaryShareImageResponse
import com.brifo.server.diary.exception.DiaryNotFoundException
import com.brifo.server.diary.exception.DiaryShareImageGenerationFailedException
import com.brifo.server.diary.repository.DiaryEntryRepository
import com.brifo.server.diary.share.DiaryShareImageModel
import com.brifo.server.diary.share.DiaryShareImageRenderer
import com.brifo.server.diary.share.ShareImageStorage
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DiaryShareImageService(
    private val diaryEntryRepository: DiaryEntryRepository,
    private val renderer: DiaryShareImageRenderer,
    private val storage: ShareImageStorage,
    private val transactionService: DiaryShareImageTransactionService,
) {
    fun create(
        userPublicId: UUID,
        diaryPublicId: UUID,
    ): CreateDiaryShareImageResponse {
        val row =
            diaryEntryRepository.findDiaryDetail(userPublicId, diaryPublicId)
                ?: throw DiaryNotFoundException()
        row.shareImageUrl?.let { existingUrl ->
            return CreateDiaryShareImageResponse(diaryPublicId, existingUrl, reused = true)
        }

        val model =
            DiaryShareImageModel(
                diaryId = row.diaryId,
                stockName = row.stockName,
                changeRate = row.changeRate,
                agentType = row.agentType,
                agentNickname = row.agentNickname,
                briefingDirection = row.briefingDirection,
                briefingConfidenceRate = row.briefingConfidenceRate.toInt(),
                isCorrect = row.isCorrect,
                decisionConfidenceLevel = row.confidenceLevel.toInt(),
            )
        val attached =
            transactionService.createIfAbsent(userPublicId, diaryPublicId) {
                try {
                    storage.store(diaryPublicId, renderer.render(model))
                } catch (_: Exception) {
                    throw DiaryShareImageGenerationFailedException()
                }
            }

        return CreateDiaryShareImageResponse(diaryPublicId, attached.url, attached.reused)
    }
}
