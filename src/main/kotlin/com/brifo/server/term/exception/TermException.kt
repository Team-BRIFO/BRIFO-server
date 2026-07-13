package com.brifo.server.term.exception

import com.brifo.server.term.code.TermErrorCode

import com.brifo.server.global.exception.BusinessException

sealed class TermException(
    errorCode: TermErrorCode,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)
