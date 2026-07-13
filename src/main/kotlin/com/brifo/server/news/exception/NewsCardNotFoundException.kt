package com.brifo.server.news.exception

import com.brifo.server.news.code.NewsCardErrorCode

class NewsCardNotFoundException(
    message: String = NewsCardErrorCode.NEWS_CARD_NOT_FOUND.message,
) : NewsCardException(
    errorCode = NewsCardErrorCode.NEWS_CARD_NOT_FOUND,
    message = message,
)
