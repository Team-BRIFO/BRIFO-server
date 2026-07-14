package com.brifo.server.badge.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class BadgeErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    BADGE_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "BADGE_404",
        "뱃지를 찾을 수 없습니다.",
    ),
}
