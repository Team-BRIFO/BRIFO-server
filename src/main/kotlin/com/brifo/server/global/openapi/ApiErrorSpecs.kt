package com.brifo.server.global.openapi

import com.brifo.server.auth.code.AuthErrorCode
import com.brifo.server.global.code.ErrorCode
import com.brifo.server.global.openapi.ApiDocumentationModels.ErrorSpec
import com.brifo.server.stock.code.StockErrorCode
import com.brifo.server.user.code.OnboardingErrorCode
import com.brifo.server.user.code.UserErrorCode

object ApiErrorSpecs {
    val invalidRequest = ErrorSpec(ErrorCode.INVALID_REQUEST, "요청 값의 형식이 잘못되었거나 유효성 검증에 실패한 경우")
    val internalServerError = ErrorSpec(ErrorCode.INTERNAL_SERVER_ERROR, "서버 내부 처리 중 예기치 않은 오류가 발생한 경우")
    val userNotFound = ErrorSpec(UserErrorCode.USER_NOT_FOUND, "인증 사용자에 해당하는 사용자가 없는 경우")
    val stockNotFound = ErrorSpec(StockErrorCode.STOCK_NOT_FOUND, "요청에 존재하지 않거나 비활성인 종목이 포함된 경우")
    val invalidNickname = ErrorSpec(UserErrorCode.INVALID_NICKNAME, "닉네임이 허용된 형식이 아닌 경우")
    val invalidCompanyName = ErrorSpec(UserErrorCode.INVALID_COMPANY_NAME, "회사명이 허용된 형식이 아닌 경우")
    val stockSelectionMinimum = ErrorSpec(StockErrorCode.STOCK_SELECTION_MINIMUM_NOT_MET, "관심 종목을 선택하지 않은 경우")
    val stockSelectionMaximum = ErrorSpec(StockErrorCode.STOCK_SELECTION_MAXIMUM_EXCEEDED, "관심 종목을 3개보다 많이 선택한 경우")
    val stockSelectionDuplicated = ErrorSpec(StockErrorCode.STOCK_SELECTION_DUPLICATED, "중복된 관심 종목이 포함된 경우")
    val onboardingAlreadyCompleted =
        ErrorSpec(OnboardingErrorCode.ONBOARDING_ALREADY_COMPLETED, "이미 온보딩을 완료한 사용자인 경우")
    val oauthRedirectMismatch =
        ErrorSpec(AuthErrorCode.OAUTH_REDIRECT_URI_MISMATCH, "요청한 Redirect URI가 서버에 등록된 URI와 다른 경우")
    val invalidAuthorizationCode =
        ErrorSpec(AuthErrorCode.OAUTH_INVALID_AUTHORIZATION_CODE, "OAuth 제공자가 인가 코드를 거부한 경우")
    val invalidOAuthToken = ErrorSpec(AuthErrorCode.INVALID_TOKEN, "OAuth 제공자가 발급한 Access Token이 유효하지 않은 경우")
    val oauthProviderFailure = ErrorSpec(AuthErrorCode.OAUTH_PROVIDER_SERVER_ERROR, "OAuth 제공자 호출 또는 응답 처리에 실패한 경우")
    val refreshTokenRequired = ErrorSpec(AuthErrorCode.REFRESH_TOKEN_REQUIRED, "Refresh Token이 없거나 빈 문자열인 경우")
    val refreshTokenExpired = ErrorSpec(AuthErrorCode.REFRESH_TOKEN_EXPIRED, "Refresh Token이 만료된 경우")
    val refreshTokenInvalid =
        ErrorSpec(AuthErrorCode.REFRESH_TOKEN_INVALID, "Refresh Token의 형식, 서명 또는 타입이 유효하지 않은 경우")
    val refreshTokenUnusable =
        ErrorSpec(AuthErrorCode.REFRESH_TOKEN_UNUSABLE, "이미 폐기되었거나 재사용된 Refresh Token인 경우")
}
