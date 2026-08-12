package com.brifo.server.diary.share

import java.util.UUID

interface ShareImageStorage {
    fun store(
        diaryId: UUID,
        image: ShareImageFile,
    ): String

    fun createDownloadUrl(key: String): String
}
