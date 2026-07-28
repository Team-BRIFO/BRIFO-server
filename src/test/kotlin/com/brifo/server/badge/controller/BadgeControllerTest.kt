package com.brifo.server.badge.controller

import com.brifo.server.authenticatedUserId
import com.brifo.server.badge.dto.response.GetBadgesResponse
import com.brifo.server.badge.dto.response.GetOwnedBadgeResponse
import com.brifo.server.badge.service.BadgeService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(BadgeController::class)
@AutoConfigureMockMvc(addFilters = false)
class BadgeControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var badgeService: BadgeService

    @Test
    fun `뱃지 목록 응답은 보유 여부를 포함한다`() {
        val userId = authenticatedUserId()
        val badgeId = UUID.randomUUID()
        `when`(badgeService.getBadges(userId)).thenReturn(
            GetBadgesResponse(
                items =
                    listOf(
                        GetBadgesResponse.Item(
                            badgeId = badgeId,
                            code = "B01",
                            name = "첫 출근",
                            isOwned = true,
                        ),
                    ),
            ),
        )

        mockMvc
            .perform(
                get("/api/badges")
                    .param("userId", userId.toString()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("COMMON_200"))
            .andExpect(jsonPath("$.result.items[0].badgeId").value(badgeId.toString()))
            .andExpect(jsonPath("$.result.items[0].code").value("B01"))
            .andExpect(jsonPath("$.result.items[0].isOwned").value(true))
    }

    @Test
    fun `보유 뱃지 상세 응답은 설명과 보상 AP를 포함한다`() {
        val userId = authenticatedUserId()
        val badgeId = UUID.randomUUID()
        `when`(badgeService.getOwnedBadge(userId, badgeId)).thenReturn(
            GetOwnedBadgeResponse(
                badgeId = badgeId,
                code = "B01",
                name = "첫 출근",
                description = "튜토리얼을 처음 완료했어요!",
                rewardAp = 50,
            ),
        )

        mockMvc
            .perform(
                get("/api/users/me/badges/{badgeId}", badgeId)
                    .param("userId", userId.toString()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.result.badgeId").value(badgeId.toString()))
            .andExpect(jsonPath("$.result.description").value("튜토리얼을 처음 완료했어요!"))
            .andExpect(jsonPath("$.result.rewardAp").value(50))
    }
}
