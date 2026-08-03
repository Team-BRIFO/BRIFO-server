package com.brifo.server.briefing.repository

import com.brifo.server.briefing.dto.response.BriefingStockResponse
import com.brifo.server.briefing.dto.response.GetStockBriefingsResponse
import com.brifo.server.briefing.dto.response.OfficeBriefingItemResponse
import com.brifo.server.briefing.entity.Briefing
import java.time.LocalDate
import java.util.UUID

interface BriefingQueryRepository {
    fun findDailyBriefings(
        userPublicId: UUID,
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): List<Briefing>

    fun findStockBriefingItems(
        userPublicId: UUID,
        stockPublicId: UUID,
        displayDate: LocalDate,
    ): List<GetStockBriefingsResponse.StockBriefingItem>

    fun findOfficeBriefings(
        userPublicId: UUID,
        displayDate: LocalDate,
    ): List<OfficeBriefingItemResponse>

    fun findStockSummary(stockPublicId: UUID): BriefingStockResponse?

    fun findOwnerPublicId(briefingPublicId: UUID): UUID?

    fun findOwnedBriefing(
        userPublicId: UUID,
        briefingPublicId: UUID,
    ): Briefing?
}
