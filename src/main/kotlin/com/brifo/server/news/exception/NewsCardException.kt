package com.brifo.server.news.exception

import com.brifo.server.news.code.NewsCardErrorCode

import com.brifo.server.global.exception.BusinessException

sealed class NewsCardException(
    errorCode: NewsCardErrorCode,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)
