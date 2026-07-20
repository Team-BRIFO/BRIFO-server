package com.brifo.server.global.exception

import com.brifo.server.global.code.BaseCode

open class BusinessException(
    val errorCode: BaseCode,
    override val message: String = errorCode.message,
) : RuntimeException(message)
