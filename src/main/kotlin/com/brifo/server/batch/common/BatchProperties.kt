package com.brifo.server.batch.common

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.batch")
data class BatchProperties(
    val schedulingEnabled: Boolean = false,
    val restartDelay: Duration = Duration.ofMinutes(10),
    val maxExecutions: Int = 3,
) {
    init {
        require(!restartDelay.isNegative && !restartDelay.isZero) { "restartDelay는 양수여야 합니다." }
        require(maxExecutions >= 1) { "maxExecutions는 1 이상이어야 합니다." }
    }
}
