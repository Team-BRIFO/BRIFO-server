package com.brifo.server.agent.code

import com.brifo.server.global.code.BaseCode
import org.springframework.http.HttpStatus

enum class AgentErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseCode {
    AGENT_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "AGENT_404",
        "사원을 찾을 수 없습니다.",
    ),
}
