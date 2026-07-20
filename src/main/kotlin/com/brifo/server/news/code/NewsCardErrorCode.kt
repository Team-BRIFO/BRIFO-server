package com.brifo.server.news.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class NewsCardErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    NEWS_CARD_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "CARD_404",
        "카드뉴스를 찾을 수 없습니다.",
    ),
}
