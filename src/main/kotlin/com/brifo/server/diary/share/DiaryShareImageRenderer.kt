package com.brifo.server.diary.share

fun interface DiaryShareImageRenderer {
    fun render(model: DiaryShareImageModel): ShareImageFile
}
