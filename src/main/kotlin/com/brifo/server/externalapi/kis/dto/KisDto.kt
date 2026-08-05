package com.brifo.server.externalapi.kis.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate

data class KisTokenRequest(
    @JsonProperty("grant_type")
    val grantType: String = "client_credentials",

    @JsonProperty("appkey")
    val appKey: String,

    @JsonProperty("appsecret")
    val appSecret: String,
)

data class KisTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,

    @JsonProperty("token_type")
    val tokenType: String,

    @JsonProperty("expires_in")
    val expiresIn: Long,
)

data class KisCurrentPriceResponse(
    @JsonProperty("rt_cd")
    val resultCode: String,

    @JsonProperty("msg1")
    val message: String?,

    val output: KisCurrentPriceOutput?,
)

data class KisCurrentPriceOutput(
    @JsonProperty("stck_prpr")
    val currentPrice: String,

    @JsonProperty("prdy_vrss")
    val priceChange: String,

    @JsonProperty("prdy_ctrt")
    val changeRate: String,
)

data class KisCurrentPriceResult(
    val stockCode: String,
    val currentPrice: BigDecimal,
    val priceChange: BigDecimal,
    val changeRate: BigDecimal,
)

data class KisDailyPriceResponse(
    @JsonProperty("rt_cd")
    val resultCode: String,

    @JsonProperty("msg1")
    val message: String?,

    val output: List<KisDailyPriceOutput> = emptyList(),
)

data class KisDailyPriceOutput(
    @JsonProperty("stck_bsop_date")
    val tradeDate: String,

    @JsonProperty("stck_clpr")
    val closingPrice: String,

    @JsonProperty("prdy_ctrt")
    val changeRate: String,
)

data class KisDailyPriceResult(
    val stockCode: String,
    val tradeDate: LocalDate,
    val price: BigDecimal,
    val changeRate: BigDecimal,
)
