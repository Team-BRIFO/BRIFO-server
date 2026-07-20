package com.brifo.server.diary.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class DiaryErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    DIARY_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "DIARY_404",
        "결정일기를 찾을 수 없습니다.",
    ),
    SHARE_IMAGE_GENERATION_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "DIARY_500_01",
        "공유 이미지 생성에 실패했습니다.",
    ),
}
