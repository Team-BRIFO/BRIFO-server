package com.brifo.server.briefing.service

import com.brifo.server.briefing.dto.response.GetOfficeBriefingsResponse
import com.brifo.server.briefing.entity.Briefing
import com.brifo.server.briefing.repository.BriefingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BriefingService(
    private val briefingRepository: BriefingRepository,
) {
    @Transactional(readOnly = true)
    fun getOfficeBriefings(): GetOfficeBriefingsResponse {
        val items =
            briefingRepository.findAllByOrderByCreatedAtDesc()
                .groupBy { requireNotNull(it.newsCard.news.stock.id) }
                .values
                .map { briefings -> briefings.toOfficeItem() }

        return GetOfficeBriefingsResponse(items = items)
    }

    private fun List<Briefing>.toOfficeItem(): GetOfficeBriefingsResponse.Item {
        val stock = first().newsCard.news.stock

        return GetOfficeBriefingsResponse.Item(
            stockName = stock.name,
            agents =
                map { briefing ->
                    GetOfficeBriefingsResponse.Agent(
                        briefingId = requireNotNull(briefing.publicId),
                        agentId = requireNotNull(briefing.agent.publicId),
                        nickname = briefing.agent.nickname,
                        agentType = briefing.agent.agentType,
                        status = briefing.status,
                    )
                },
        )
    }
}
