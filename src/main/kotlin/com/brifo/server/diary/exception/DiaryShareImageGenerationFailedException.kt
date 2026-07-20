package com.brifo.server.diary.exception

import com.brifo.server.diary.code.DiaryErrorCode

class DiaryShareImageGenerationFailedException(
    message: String = DiaryErrorCode.SHARE_IMAGE_GENERATION_FAILED.message,
) : DiaryException(
    errorCode = DiaryErrorCode.SHARE_IMAGE_GENERATION_FAILED,
    message = message,
)
