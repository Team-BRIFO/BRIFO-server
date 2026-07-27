package com.brifo.server.externalapi.kis.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate

// KIS 토큰 발급 요청
data class KisTokenRequest(
    @JsonProperty("grant_type")
    val grantType: String = "client_credentials",

    @JsonProperty("appkey")
    val appKey: String,

    @JsonProperty("appsecret")
    val appSecret: String,
)

// KIS 토큰 발급 응답
data class KisTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,

    @JsonProperty("token_type")
    val tokenType: String,

    @JsonProperty("expires_in")
    val expiresIn: Long,
)

// KIS 현재가 전체 응답
data class KisCurrentPriceResponse(
    @JsonProperty("rt_cd")
    val resultCode: String,

    @JsonProperty("msg1")
    val message: String?,

    val output: KisCurrentPriceOutput?,
)

// KIS 현재가 응답의 실제 가격 부분
data class KisCurrentPriceOutput(
    @JsonProperty("stck_prpr")
    val currentPrice: String,

    @JsonProperty("prdy_vrss")
    val priceChange: String,

    @JsonProperty("prdy_ctrt")
    val changeRate: String,
)

// 서비스에 반환할 현재가 결과
data class KisCurrentPriceResult(
    val stockCode: String,
    val currentPrice: BigDecimal,
    val priceChange: BigDecimal,
    val changeRate: BigDecimal,
)

// KIS 기간별 시세 전체 응답
data class KisDailyPriceResponse(
    @JsonProperty("rt_cd")
    val resultCode: String,

    @JsonProperty("msg1")
    val message: String?,

    val output: List<KisDailyPriceOutput> = emptyList(),
)

// 기간별 시세의 일별 가격
data class KisDailyPriceOutput(
    @JsonProperty("stck_bsop_date")
    val tradeDate: String,

    @JsonProperty("stck_clpr")
    val closingPrice: String,

    @JsonProperty("prdy_ctrt")
    val changeRate: String,
)

// Batch에 반환할 종가 결과
data class KisDailyPriceResult(
    val stockCode: String,
    val tradeDate: LocalDate,
    val price: BigDecimal,
    val changeRate: BigDecimal,
)
