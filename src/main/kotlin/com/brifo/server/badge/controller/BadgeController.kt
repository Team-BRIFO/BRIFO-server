package com.brifo.server.badge.controller

import com.brifo.server.badge.dto.response.GetBadgesResponse
import com.brifo.server.badge.dto.response.GetOwnedBadgeResponse
import com.brifo.server.badge.service.BadgeService
import com.brifo.server.global.common.ApiResponse
import com.brifo.server.global.code.SuccessCode
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
    fun getBadges(
        @AuthenticationPrincipal userPublicId: UUID,
    ): ApiResponse<GetBadgesResponse> =
        ApiResponse.success(
            code = SuccessCode.OK,
            result = badgeService.getBadges(userPublicId),
        )

    @GetMapping("/users/me/badges/{badgeId}")
    fun getOwnedBadge(
        @AuthenticationPrincipal userPublicId: UUID,
        @PathVariable badgeId: UUID,
    ): ApiResponse<GetOwnedBadgeResponse> =
        ApiResponse.success(
            code = SuccessCode.OK,
            result = badgeService.getOwnedBadge(userPublicId, badgeId),
        )
}
