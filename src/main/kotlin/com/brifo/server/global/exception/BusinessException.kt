package com.brifo.server.global.exception

import com.brifo.server.global.code.ErrorCode

class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
) : RuntimeException(message)
