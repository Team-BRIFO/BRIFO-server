package com.brifo.server.agent.exception

import com.brifo.server.agent.code.AgentErrorCode

import com.brifo.server.global.exception.BusinessException

sealed class AgentException(
    errorCode: AgentErrorCode,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)
