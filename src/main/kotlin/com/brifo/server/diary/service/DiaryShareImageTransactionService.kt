package com.brifo.server.diary.service

import com.brifo.server.diary.exception.DiaryNotFoundException
import com.brifo.server.diary.repository.DiaryEntryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class DiaryShareImageTransactionService(
    private val diaryEntryRepository: DiaryEntryRepository,
) {
    @Transactional
    fun attachIfAbsent(
        userPublicId: UUID,
        diaryPublicId: UUID,
        generatedUrl: String,
    ): AttachedShareImage {
        val diary =
            diaryEntryRepository.findOwnedByPublicIdForUpdate(userPublicId, diaryPublicId)
                ?: throw DiaryNotFoundException()
        val existingUrl = diary.shareImageUrl
        if (existingUrl != null) {
            return AttachedShareImage(existingUrl, reused = true)
        }

        diary.attachShareImage(generatedUrl, LocalDateTime.now())
        return AttachedShareImage(generatedUrl, reused = false)
    }
}

data class AttachedShareImage(
    val url: String,
    val reused: Boolean,
)
