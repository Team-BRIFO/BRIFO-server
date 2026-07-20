package com.brifo.server.term.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class TermErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    TERM_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "TERM_404",
        "용어를 찾을 수 없습니다.",
    ),
}
