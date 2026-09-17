package com.brifo.server.payment.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * 기본값은 토스페이먼츠 공식 문서에 공개된 범용 테스트 키다.
 * (https://docs.tosspayments.com — 가입 없이 결제 흐름을 테스트할 수 있도록 토스가 직접 배포)
 * 실제 정산이 필요해지면 자체 발급받은 키로 환경변수를 덮어쓰기만 하면 된다.
 */
@ConfigurationProperties(prefix = "external.toss")
data class TossPaymentsProperties(
    val baseUrl: String = "https://api.tosspayments.com",
    val clientKey: String = "test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq",
    val secretKey: String = "test_sk_zXLkKEypNArWmo50nX3lmeaxYG5R",
    val connectTimeout: Duration = Duration.ofSeconds(3),
    val readTimeout: Duration = Duration.ofSeconds(10),
    /** 충전 금액 하한(원). 토스 테스트 결제도 너무 작은 금액은 의미가 없어 최소치를 둔다. */
    val minChargeAmount: Int = 1_000,
    /** 충전 금액 상한(원). 실수로 큰 금액을 결제하는 것을 막는다. */
    val maxChargeAmount: Int = 1_000_000,
)
