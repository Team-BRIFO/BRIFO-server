package com.brifo.server.diary.share

import java.util.UUID

fun interface ShareImageStorage {
    fun store(
        diaryId: UUID,
        image: ShareImageFile,
    ): String
}
