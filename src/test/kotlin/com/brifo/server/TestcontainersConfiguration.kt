package com.brifo.server

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer() = PostgreSQLContainer(DockerImageName.parse("postgres:18"))

    /**
     * Valkey(Redis 호환)를 쓰는 테스트가 있는데 컨테이너가 없어, 로컬에 Redis를 띄워두지
     * 않으면 캐시·게스트 로그인 레이트리밋 테스트가 연결 실패로 깨졌다. CI 러너에도 Redis가
     * 없으므로 Postgres와 같은 방식으로 띄운다. 이미지는 docker-compose.yml 과 맞춘다.
     */
    @Bean
    @ServiceConnection(name = "redis")
    fun valkeyContainer() =
        GenericContainer(DockerImageName.parse("valkey/valkey:9.1.1-alpine"))
            .withExposedPorts(REDIS_PORT)

    private companion object {
        const val REDIS_PORT = 6379
    }
}
