package com.brifo.server.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties("app.dev-behavior")
data class DevBehaviorProperties(
    val immediateInterestStockUpdates: Boolean = false,
    val briefingTimeRestrictionsEnabled: Boolean = true,
    val decisionRequestCutoffEnabled: Boolean = true,
    val immediateDecisionSettlement: Boolean = false,
    val useTargetDateAsDisplayDate: Boolean = false,
)

@Configuration
@EnableConfigurationProperties(DevBehaviorProperties::class)
class DevBehaviorConfiguration
