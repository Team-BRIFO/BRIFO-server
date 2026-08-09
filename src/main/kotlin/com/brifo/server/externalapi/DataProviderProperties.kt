package com.brifo.server.externalapi

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.data-provider")
data class DataProviderProperties(
    val type: DataProviderType = DataProviderType.DATA_SERVER,
)

enum class DataProviderType {
    DATA_SERVER,
    KIS,
}
