package com.brifo.server.global.openapi

import com.brifo.server.agent.code.AgentErrorCode
import com.brifo.server.ap.code.ApErrorCode
import com.brifo.server.auth.code.AuthErrorCode
import com.brifo.server.badge.code.BadgeErrorCode
import com.brifo.server.briefing.code.BriefingErrorCode
import com.brifo.server.decision.code.DecisionErrorCode
import com.brifo.server.diary.code.DiaryErrorCode
import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.openapi.ApiDocumentationModels.Endpoint
import com.brifo.server.global.openapi.ApiDocumentationModels.ErrorSpec
import com.brifo.server.global.openapi.ApiDocumentationModels.OperationSpec
import com.brifo.server.news.code.NewsCardErrorCode
import com.brifo.server.policy.code.PolicyErrorCode
import com.brifo.server.stock.code.StockErrorCode
import com.brifo.server.term.code.TermErrorCode
import com.brifo.server.user.code.OnboardingErrorCode
import org.springframework.http.HttpMethod.DELETE
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.PATCH
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpMethod.PUT

object ApiErrorCatalog {
    val publicOperations =
        setOf(
            Endpoint(POST, "/api/auth/login/kakao"),
            Endpoint(POST, "/api/auth/login/naver"),
            Endpoint(POST, "/api/auth/reissue"),
        )

    val operations: List<OperationSpec>
        get() =
            with(ApiErrorSpecs) {
                listOf(
                    // Agent
                    OperationSpec(GET, "/api/agents"),
                    OperationSpec(
                        GET,
                        "/api/agents/{agentId}",
                        invalidRequest,
                        ErrorSpec(AgentErrorCode.AGENT_NOT_FOUND, "사원 ID에 해당하는 사원이 없는 경우"),
                    ),
                    // AP
                    OperationSpec(GET, "/api/ap/transactions", invalidRequest, userNotFound),
                    OperationSpec(
                        POST,
                        "/api/ap/attendance-rewards",
                        userNotFound,
                        ErrorSpec(ApErrorCode.ATTENDANCE_REWARD_ALREADY_CLAIMED, "오늘 출석 보상을 이미 받은 경우"),
                    ),
                    OperationSpec(
                        POST,
                        "/api/ap/tutorial-rewards",
                        userNotFound,
                        ErrorSpec(ApErrorCode.TUTORIAL_REWARD_ALREADY_CLAIMED, "튜토리얼 보상을 이미 받은 경우"),
                    ),
                    OperationSpec(
                        POST,
                        "/api/ap/credit-loans",
                        invalidRequest,
                        userNotFound,
                        ErrorSpec(AgentErrorCode.AGENT_NOT_FOUND, "요청한 사원이 인증 사용자의 사원이 아닌 경우"),
                        ErrorSpec(ApErrorCode.CREDIT_LOAN_ALREADY_CLAIMED, "신용대출을 이미 사용한 경우"),
                        ErrorSpec(ApErrorCode.CREDIT_LOAN_NOT_ELIGIBLE, "신용대출 조건을 충족하지 못한 경우"),
                    ),
                    // Auth
                    OperationSpec(
                        POST,
                        "/api/auth/login/kakao",
                        invalidRequest,
                        oauthRedirectMismatch,
                        invalidAuthorizationCode,
                        invalidOAuthToken,
                        oauthProviderFailure,
                    ),
                    OperationSpec(
                        POST,
                        "/api/auth/login/naver",
                        invalidRequest,
                        oauthRedirectMismatch,
                        invalidAuthorizationCode,
                        invalidOAuthToken,
                        oauthProviderFailure,
                    ),
                    OperationSpec(
                        POST,
                        "/api/auth/logout",
                        invalidRequest,
                        refreshTokenRequired,
                        refreshTokenExpired,
                        refreshTokenInvalid,
                        refreshTokenUnusable,
                        ErrorSpec(AuthErrorCode.REFRESH_TOKEN_MISMATCH, "Access Token과 Refresh Token의 사용자가 다른 경우"),
                    ),
                    OperationSpec(
                        POST,
                        "/api/auth/reissue",
                        invalidRequest,
                        refreshTokenRequired,
                        refreshTokenExpired,
                        refreshTokenInvalid,
                        refreshTokenUnusable,
                        ErrorSpec(AuthErrorCode.AUTH_USER_NOT_FOUND, "Refresh Token에 해당하는 사용자가 없는 경우"),
                    ),
                    // Badge
                    OperationSpec(GET, "/api/badges"),
                    OperationSpec(
                        GET,
                        "/api/users/me/badges/{badgeId}",
                        invalidRequest,
                        ErrorSpec(BadgeErrorCode.BADGE_NOT_FOUND, "보유한 뱃지가 없는 경우"),
                    ),
                    // Briefing
                    OperationSpec(
                        POST,
                        "/api/stocks/{stockId}/briefings",
                        invalidRequest,
                        userNotFound,
                        ErrorSpec(StockErrorCode.STOCK_NOT_FOUND, "요청한 종목이 없거나 활성 상태가 아닌 경우"),
                        ErrorSpec(NewsCardErrorCode.NEWS_CARD_NOT_FOUND, "요청에 필요한 카드뉴스가 없는 경우"),
                        ErrorSpec(AgentErrorCode.AGENT_NOT_FOUND, "요청한 사원이 없거나 인증 사용자의 사원이 아닌 경우"),
                        ErrorSpec(BriefingErrorCode.BRIEFING_ALREADY_REQUESTED, "동일 사원의 브리핑을 이미 요청한 경우"),
                        ErrorSpec(
                            BriefingErrorCode.BRIEFING_AGENT_NOT_IN_INITIAL_REQUEST,
                            "재의뢰 시 최초 의뢰에 포함되지 않은 사원을 추가한 경우",
                        ),
                        ErrorSpec(BriefingErrorCode.BRIEFING_REQUEST_CLOSED, "브리핑 의뢰 마감 시간 이후 요청한 경우"),
                        ErrorSpec(ApErrorCode.INSUFFICIENT_AP_BALANCE, "브리핑 비용을 지불할 AP가 부족한 경우"),
                        ErrorSpec(BriefingErrorCode.BRIEFING_RETRY_COOLDOWN, "실패한 브리핑을 재의뢰할 수 있는 대기 시간이 지나지 않은 경우"),
                    ),
                    OperationSpec(
                        GET,
                        "/api/stocks/{stockId}/briefing",
                        invalidRequest,
                        ErrorSpec(StockErrorCode.STOCK_NOT_FOUND, "요청한 종목이 없거나 활성 상태가 아닌 경우"),
                        ErrorSpec(BriefingErrorCode.BRIEFING_NOT_FOUND, "조회할 브리핑이 없는 경우"),
                    ),
                    OperationSpec(
                        GET,
                        "/api/briefings/{briefingId}",
                        invalidRequest,
                        ErrorSpec(BriefingErrorCode.BRIEFING_NOT_FOUND, "브리핑이 없거나 인증 사용자의 브리핑이 아닌 경우"),
                        ErrorSpec(BriefingErrorCode.BRIEFING_NOT_COMPLETED, "브리핑 분석이 아직 완료되지 않은 경우"),
                        ErrorSpec(BriefingErrorCode.BRIEFING_PROCESSING_FAILED, "브리핑 분석이 실패한 경우"),
                    ),
                    OperationSpec(GET, "/api/briefings/office"),
                    // Decision
                    OperationSpec(
                        POST,
                        "/api/briefings/{briefingId}/decisions",
                        invalidRequest,
                        ErrorSpec(BriefingErrorCode.BRIEFING_NOT_FOUND, "브리핑이 없거나 인증 사용자의 브리핑이 아닌 경우"),
                        ErrorSpec(BriefingErrorCode.BRIEFING_NOT_COMPLETED, "브리핑 분석이 아직 완료되지 않은 경우"),
                        ErrorSpec(DecisionErrorCode.DECISION_ALREADY_EXISTS, "해당 브리핑에 대한 예측을 이미 등록한 경우"),
                        ErrorSpec(DecisionErrorCode.DECISION_REQUEST_CLOSED, "결정 등록 마감 시간 이후 요청한 경우"),
                    ),
                    OperationSpec(GET, "/api/decisions"),
                    OperationSpec(
                        GET,
                        "/api/decisions/{decisionId}",
                        invalidRequest,
                        ErrorSpec(DecisionErrorCode.DECISION_NOT_FOUND, "예측이 없거나 인증 사용자의 예측이 아닌 경우"),
                        ErrorSpec(DecisionErrorCode.DECISION_NOT_SETTLED, "예측이 아직 정산되지 않은 경우"),
                    ),
                    // Diary
                    OperationSpec(GET, "/api/diaries", invalidRequest),
                    OperationSpec(GET, "/api/diaries/calendar", invalidRequest),
                    OperationSpec(
                        GET,
                        "/api/diaries/{diaryId}",
                        invalidRequest,
                        ErrorSpec(DiaryErrorCode.DIARY_NOT_FOUND, "결정일기가 없거나 인증 사용자의 결정일기가 아닌 경우"),
                    ),
                    OperationSpec(GET, "/api/diaries/stats"),
                    OperationSpec(POST, "/api/diaries/{diaryId}/share-images", invalidRequest),
                    // News
                    OperationSpec(
                        GET,
                        "/api/stocks/{stockId}/news-cards",
                        invalidRequest,
                        ErrorSpec(NewsCardErrorCode.NEWS_CARD_NOT_FOUND, "해당 종목의 카드뉴스가 없는 경우"),
                        ErrorSpec(ErrorCode.INTERNAL_SERVER_ERROR, "카드뉴스의 기준 가격 데이터가 없는 경우"),
                    ),
                    // Notification
                    OperationSpec(GET, "/api/notifications", invalidRequest),
                    // Policy
                    OperationSpec(GET, "/api/policies", userNotFound),
                    OperationSpec(
                        GET,
                        "/api/policies/{policyId}",
                        invalidRequest,
                        ErrorSpec(PolicyErrorCode.POLICY_NOT_FOUND, "활성 약관이 없는 경우"),
                    ),
                    OperationSpec(
                        POST,
                        "/api/users/me/policies",
                        invalidRequest,
                        userNotFound,
                        ErrorSpec(PolicyErrorCode.POLICY_NOT_FOUND, "요청에 존재하지 않거나 비활성인 약관이 포함된 경우"),
                        ErrorSpec(PolicyErrorCode.REQUIRED_POLICY_MISSING, "필수 약관 동의가 누락된 경우"),
                    ),
                    OperationSpec(GET, "/api/users/me/policies/pending", userNotFound),
                    OperationSpec(
                        DELETE,
                        "/api/users/me/policies/{policyId}",
                        invalidRequest,
                        userNotFound,
                        ErrorSpec(PolicyErrorCode.POLICY_NOT_FOUND, "존재하지 않거나 비활성인 약관인 경우"),
                        ErrorSpec(PolicyErrorCode.REQUIRED_POLICY_CANNOT_BE_REVOKED, "필수 약관을 철회하려는 경우"),
                    ),
                    // Stock
                    OperationSpec(GET, "/api/stocks", invalidRequest),
                    // Term
                    OperationSpec(GET, "/api/users/me/terms", invalidRequest, userNotFound),
                    OperationSpec(
                        GET,
                        "/api/terms/{termId}",
                        invalidRequest,
                        userNotFound,
                        ErrorSpec(TermErrorCode.TERM_NOT_FOUND, "용어가 존재하지 않는 경우"),
                    ),
                    OperationSpec(
                        PUT,
                        "/api/users/me/terms/{termId}",
                        invalidRequest,
                        userNotFound,
                        ErrorSpec(TermErrorCode.TERM_NOT_FOUND, "용어가 존재하지 않는 경우"),
                    ),
                    // User
                    OperationSpec(
                        PATCH,
                        "/api/onboarding/profile",
                        invalidRequest,
                        invalidNickname,
                        invalidCompanyName,
                        stockSelectionMinimum,
                        stockSelectionMaximum,
                        stockSelectionDuplicated,
                        userNotFound,
                        stockNotFound,
                        onboardingAlreadyCompleted,
                    ),
                    OperationSpec(
                        POST,
                        "/api/onboarding/complete",
                        userNotFound,
                        ErrorSpec(OnboardingErrorCode.REQUIRED_POLICIES_NOT_AGREED, "필수 약관 동의가 완료되지 않은 경우"),
                        ErrorSpec(OnboardingErrorCode.PROFILE_NOT_COMPLETED, "온보딩 프로필 입력이 완료되지 않은 경우"),
                        ErrorSpec(OnboardingErrorCode.STOCKS_NOT_SELECTED, "관심 종목 선택이 완료되지 않은 경우"),
                        onboardingAlreadyCompleted,
                    ),
                    OperationSpec(GET, "/api/users/me/profile", userNotFound),
                    OperationSpec(GET, "/api/users/me", userNotFound),
                    OperationSpec(
                        PATCH,
                        "/api/users/me/profile",
                        invalidRequest,
                        invalidNickname,
                        invalidCompanyName,
                        stockSelectionMinimum,
                        stockSelectionMaximum,
                        stockSelectionDuplicated,
                        userNotFound,
                        stockNotFound,
                    ),
                    OperationSpec(DELETE, "/api/users/me", userNotFound),
                    OperationSpec(GET, "/api/users/me/home", userNotFound),
                )
            }
}
