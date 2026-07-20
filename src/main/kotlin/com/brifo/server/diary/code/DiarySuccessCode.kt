package com.brifo.server.diary.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class DiarySuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    SHARE_IMAGE_CREATED(
        HttpStatus.OK,
        "DIARY_200_01",
        "공유 이미지가 생성되었습니다.",
    ),
}
