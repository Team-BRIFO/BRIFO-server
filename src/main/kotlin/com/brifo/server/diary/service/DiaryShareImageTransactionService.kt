package com.brifo.server.diary.service

import com.brifo.server.diary.exception.DiaryNotFoundException
import com.brifo.server.diary.repository.DiaryEntryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class DiaryShareImageTransactionService(
    private val diaryEntryRepository: DiaryEntryRepository,
    private val clock: Clock,
) {
    @Transactional
    fun createIfAbsent(
        userPublicId: UUID,
        diaryPublicId: UUID,
        generateUrl: () -> String,
    ): AttachedShareImage {
        val diary =
            diaryEntryRepository.findOwnedByPublicIdForUpdate(userPublicId, diaryPublicId)
                ?: throw DiaryNotFoundException()
        val existingUrl = diary.shareImageUrl
        if (existingUrl != null) {
            return AttachedShareImage(existingUrl, reused = true)
        }

        val generatedUrl = generateUrl()
        diary.attachShareImage(generatedUrl, LocalDateTime.now(clock))
        return AttachedShareImage(generatedUrl, reused = false)
    }
}

data class AttachedShareImage(
    val url: String,
    val reused: Boolean,
)
