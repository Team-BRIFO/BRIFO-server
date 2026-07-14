package com.brifo.server.badge.controller

import com.brifo.server.badge.dto.response.GetBadgesResponse
import com.brifo.server.badge.dto.response.GetOwnedBadgeResponse
import com.brifo.server.badge.service.BadgeService
import com.brifo.server.global.common.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class BadgeController(
    private val badgeService: BadgeService,
) {
    @GetMapping("/badges")
    fun getBadges(): ApiResponse<GetBadgesResponse> = TODO("Badge 목록 조회 서비스 구현 필요")

    @GetMapping("/users/me/badges/{badgeId}")
    fun getOwnedBadge(
        @PathVariable badgeId: UUID,
    ): ApiResponse<GetOwnedBadgeResponse> = TODO("보유 Badge 상세 조회 서비스 구현 필요")
}
