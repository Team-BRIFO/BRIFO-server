package com.brifo.server.policy.controller

import com.brifo.server.policy.dto.response.GetPendingPoliciesResponse
import com.brifo.server.policy.dto.response.GetPoliciesResponse
import com.brifo.server.policy.dto.response.GetPolicyDetailResponse
import com.brifo.server.policy.exception.PolicyNotFoundException
import com.brifo.server.policy.exception.RequiredPolicyCannotBeRevokedException
import com.brifo.server.policy.exception.RequiredPolicyMissingException
import com.brifo.server.policy.service.PolicyService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(PolicyController::class)
@AutoConfigureMockMvc(addFilters = false)
class PolicyControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var policyService: PolicyService

    @Test
    fun `약관 목록 조회는 공개 사용자 ID를 서비스에 전달한다`() {
        val userId = UUID.randomUUID()
        val policyId = UUID.randomUUID()
        `when`(policyService.getPolicies(userId)).thenReturn(
            GetPoliciesResponse(
                listOf(GetPoliciesResponse.Item(policyId, "서비스 이용약관", true, true)),
            ),
        )

        mockMvc
            .perform(get("/api/policies").param("userId", userId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("COMMON_200"))
            .andExpect(jsonPath("$.result.items[0].policyId").value(policyId.toString()))
            .andExpect(jsonPath("$.result.items[0].isAgreed").value(true))

        verify(policyService).getPolicies(userId)
    }

    @Test
    fun `약관 동의는 공개 사용자 ID와 요청 약관을 서비스에 전달한다`() {
        val userId = UUID.randomUUID()
        val policyId = UUID.randomUUID()

        mockMvc
            .perform(
                post("/api/users/me/policies")
                    .param("userId", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"policyIds":["$policyId"]}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.result").doesNotExist())

        verify(policyService).agreePolicies(userId, listOf(policyId))
    }

    @Test
    fun `약관 ID가 없거나 빈 동의 요청은 COMMON_400이다`() {
        listOf("{}", """{"policyIds":[]}""").forEach { body ->
            mockMvc
                .perform(
                    post("/api/users/me/policies")
                        .param("userId", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("COMMON_400"))
        }
    }

    @Test
    fun `pending 조회는 공개 사용자 ID를 서비스에 전달한다`() {
        val userId = UUID.randomUUID()
        val policyId = UUID.randomUUID()
        `when`(policyService.getPendingPolicies(userId)).thenReturn(
            GetPendingPoliciesResponse(
                listOf(GetPendingPoliciesResponse.Item(policyId, "필수 약관", true, BigDecimal("2.00"))),
            ),
        )

        mockMvc
            .perform(get("/api/users/me/policies/pending").param("userId", userId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.items[0].policyId").value(policyId.toString()))

        verify(policyService).getPendingPolicies(userId)
    }

    @Test
    fun `선택 약관 철회는 공개 사용자 ID와 약관 ID를 서비스에 전달한다`() {
        val userId = UUID.randomUUID()
        val policyId = UUID.randomUUID()
        mockMvc
            .perform(
                delete("/api/users/me/policies/{policyId}", policyId)
                    .param("userId", userId.toString()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.result").doesNotExist())

        verify(policyService).revokePolicy(userId, policyId)
    }

    @Test
    fun `잘못된 사용자 또는 약관 UUID는 COMMON_400이다`() {
        mockMvc
            .perform(get("/api/policies"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))

        mockMvc
            .perform(get("/api/policies").param("userId", "invalid"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))

        mockMvc
            .perform(get("/api/policies/{policyId}", "invalid"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))
    }

    @Test
    fun `약관 상세 조회의 도메인 예외는 POLICY_404 응답이다`() {
        val policyId = UUID.randomUUID()
        `when`(policyService.getPolicyDetail(policyId)).thenThrow(PolicyNotFoundException())

        mockMvc
            .perform(get("/api/policies/{policyId}", policyId))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("POLICY_404"))
    }

    @Test
    fun `필수 약관 누락 예외는 POLICY_400_01 응답이다`() {
        val userId = UUID.randomUUID()
        val policyId = UUID.randomUUID()
        doThrow(RequiredPolicyMissingException())
            .`when`(policyService)
            .agreePolicies(userId, listOf(policyId))

        mockMvc
            .perform(
                post("/api/users/me/policies")
                    .param("userId", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"policyIds":["$policyId"]}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("POLICY_400_01"))
    }

    @Test
    fun `필수 약관 철회 예외는 POLICY_400_02 응답이다`() {
        val userId = UUID.randomUUID()
        val policyId = UUID.randomUUID()
        doThrow(RequiredPolicyCannotBeRevokedException())
            .`when`(policyService)
            .revokePolicy(userId, policyId)

        mockMvc
            .perform(
                delete("/api/users/me/policies/{policyId}", policyId)
                    .param("userId", userId.toString()),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("POLICY_400_02"))
    }
}
