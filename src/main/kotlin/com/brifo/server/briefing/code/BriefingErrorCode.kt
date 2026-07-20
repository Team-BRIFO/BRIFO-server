package com.brifo.server.briefing.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class BriefingErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    BRIEFING_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "BRIEFING_404",
        "브리핑을 찾을 수 없습니다.",
    ),
    BRIEFING_ALREADY_REQUESTED(
        HttpStatus.CONFLICT,
        "BRIEFING_409_01",
        "이미 요청한 브리핑입니다.",
    ),
    BRIEFING_NOT_COMPLETED(
        HttpStatus.CONFLICT,
        "BRIEFING_409_02",
        "브리핑이 아직 완료되지 않았습니다.",
    ),
    BRIEFING_LIMIT_EXCEEDED(
        HttpStatus.CONFLICT,
        "BRIEFING_409_03",
        "카드뉴스당 브리핑 요청 한도를 초과했습니다.",
    ),
    BRIEFING_PROCESSING_FAILED(
        HttpStatus.CONFLICT,
        "BRIEFING_409_04",
        "브리핑 처리에 실패했습니다.",
    ),
}
