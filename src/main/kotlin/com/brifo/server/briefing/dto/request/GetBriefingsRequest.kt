package com.brifo.server.briefing.dto.request

data class GetBriefingsRequest(
    val status: Status = Status.ALL,
) {
    enum class Status {
        ALL,
        COMPLETED,
    }
}
