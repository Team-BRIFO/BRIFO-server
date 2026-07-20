package com.brifo.server.term.exception

import com.brifo.server.term.code.TermErrorCode

class TermNotFoundException(
    message: String = TermErrorCode.TERM_NOT_FOUND.message,
) : TermException(
    errorCode = TermErrorCode.TERM_NOT_FOUND,
    message = message,
)
