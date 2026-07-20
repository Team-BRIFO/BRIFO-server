package com.brifo.server.diary.exception

import com.brifo.server.diary.code.DiaryErrorCode

class DiaryNotFoundException(
    message: String = DiaryErrorCode.DIARY_NOT_FOUND.message,
) : DiaryException(
    errorCode = DiaryErrorCode.DIARY_NOT_FOUND,
    message = message,
)
