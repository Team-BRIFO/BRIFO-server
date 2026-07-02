package com.brifo.server.global.code

import org.springframework.http.HttpStatus

enum class SuccessCode(
	override val status: HttpStatus,
	override val code: String,
	override val message: String,
) : BaseCode {
	OK(HttpStatus.OK, "COMMON_200", "요청에 성공했습니다."),
	CREATED(HttpStatus.CREATED, "COMMON_201", "요청이 성공적으로 생성되었습니다."),
	NO_CONTENT(HttpStatus.NO_CONTENT, "COMMON_204", "요청이 성공적으로 처리되었습니다."),
}
