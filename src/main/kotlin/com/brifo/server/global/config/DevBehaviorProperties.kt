package com.brifo.server.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@ConfigurationProperties("app.dev-behavior")
data class DevBehaviorProperties(
    val immediateInterestStockUpdates: Boolean = false,
    val briefingTimeRestrictionsEnabled: Boolean = true,
    val decisionRequestCutoffEnabled: Boolean = true,
    val immediateDecisionSettlement: Boolean = false,
    /** 데모데이 시연용. 주말에도 장이 열린 것처럼 브리핑·예측을 받고, 직전 거래일 종가로 즉시 정산한다. */
    val weekendMarketEnabled: Boolean = false,
)

@Configuration
@Profile("dev")
@EnableConfigurationProperties(DevBehaviorProperties::class)
class DevBehaviorConfiguration
