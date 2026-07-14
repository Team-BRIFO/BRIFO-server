package com.brifo.server.log.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// 생성자 주입을 하기 위해 ObjectMapper 객체를 Spring에 등록하는 코드
// Config가 추가된 이유: ""Spring Boot 4의 기본 Jackson 3 != Hibernate JSONB에서 사용하는 Jackson 2"" 타입이 달라 자동 주입할 수 없기 때문
@Configuration
class ExternalApiCallLogConfig {
    @Bean
    fun externalApiCallLogObjectMapper(): ObjectMapper {
        return ObjectMapper()
    }
}
