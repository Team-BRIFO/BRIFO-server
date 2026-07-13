package com.brifo.server.diary.exception

import com.brifo.server.diary.code.DiaryErrorCode

import com.brifo.server.global.exception.BusinessException

sealed class DiaryException(
    errorCode: DiaryErrorCode,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)
