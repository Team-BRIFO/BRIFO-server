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
        "종목당 브리핑 요청 한도를 초과했습니다.",
    ),
    BRIEFING_PROCESSING_FAILED(
        HttpStatus.CONFLICT,
        "BRIEFING_409_04",
        "브리핑 처리에 실패했습니다.",
    ),
    BRIEFING_AGENT_NOT_IN_INITIAL_REQUEST(
        HttpStatus.CONFLICT,
        "BRIEFING_409_05",
        "최초 의뢰에 포함되지 않은 사원은 추가할 수 없습니다.",
    ),
    BRIEFING_REQUEST_CLOSED(
        HttpStatus.CONFLICT,
        "BRIEFING_409_06",
        "오늘의 브리핑 의뢰 시간이 마감되었습니다.",
    ),
    BRIEFING_RETRY_COOLDOWN(
        HttpStatus.TOO_MANY_REQUESTS,
        "BRIEFING_429_01",
        "브리핑을 다시 의뢰하기 전에 잠시 기다려 주세요.",
    ),
}
