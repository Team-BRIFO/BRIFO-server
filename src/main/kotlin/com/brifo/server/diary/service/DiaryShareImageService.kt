package com.brifo.server.diary.service

import com.brifo.server.diary.dto.response.CreateDiaryShareImageResponse
import com.brifo.server.diary.exception.DiaryNotFoundException
import com.brifo.server.diary.exception.DiaryShareImageGenerationFailedException
import com.brifo.server.diary.repository.DiaryEntryRepository
import com.brifo.server.diary.share.DiaryShareImageModel
import com.brifo.server.diary.share.DiaryShareImageRenderer
import com.brifo.server.diary.share.ShareImageStorage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.RoundingMode
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
        row.shareImageUrl?.takeIf { it.isShareImageObjectKey(diaryPublicId) }?.let { existingKey ->
            return CreateDiaryShareImageResponse(
                diaryPublicId,
                createDownloadUrl(diaryPublicId, existingKey),
                reused = true,
                changeRate = row.changeRate.setScale(1, RoundingMode.HALF_UP),
                tradeDate = row.tradeDate,
                apDelta = row.apDelta,
            )
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
                    val file = renderer.render(model)
                    storage.store(diaryPublicId, file)
                } catch (exception: Exception) {
                    logger.error("Failed to generate diary share image: diaryId={}", diaryPublicId, exception)
                    throw DiaryShareImageGenerationFailedException()
                }
            }

        return CreateDiaryShareImageResponse(
            diaryPublicId,
            createDownloadUrl(diaryPublicId, attached.url),
            attached.reused,
            row.changeRate.setScale(1, RoundingMode.HALF_UP),
            row.tradeDate,
            row.apDelta,
        )
    }

    private fun createDownloadUrl(
        diaryPublicId: UUID,
        key: String,
    ): String =
        try {
            storage.createDownloadUrl(key)
        } catch (exception: Exception) {
            logger.error("Failed to presign diary share image: diaryId={}", diaryPublicId, exception)
            throw DiaryShareImageGenerationFailedException()
        }

    companion object {
        private val logger = LoggerFactory.getLogger(DiaryShareImageService::class.java)
    }
}

internal fun String.isShareImageObjectKey(diaryPublicId: UUID): Boolean =
    startsWith("$diaryPublicId.") && substringAfterLast('.').matches(Regex("[A-Za-z0-9]+"))
