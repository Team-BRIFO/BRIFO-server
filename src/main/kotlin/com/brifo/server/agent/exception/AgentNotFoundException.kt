package com.brifo.server.agent.exception

import com.brifo.server.agent.code.AgentErrorCode

class AgentNotFoundException(
    message: String = AgentErrorCode.AGENT_NOT_FOUND.message,
) : AgentException(
    errorCode = AgentErrorCode.AGENT_NOT_FOUND,
    message = message,
)
