package com.brifo.server.global.external

import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class ExternalRestClientFactory {
    // 같은 주소와 정책의 RestClient는 재사용한다.
    private val clients = ConcurrentHashMap<ClientKey, RestClient>()

    fun get(
        baseUrl: String,
        policy: ExternalApiCallPolicy,
    ): RestClient {
        val key = ClientKey(baseUrl, policy)

        return clients.computeIfAbsent(key) {
            createRestClient(baseUrl, policy.timeout)
        }
    }

    // timeout이 설정된 RestClient를 생성한다.
    private fun createRestClient(
        baseUrl: String,
        timeout: Duration,
    ): RestClient {
        return RestClient
            .builder()
            .baseUrl(baseUrl)
            .requestFactory(createRequestFactory(timeout))
            .build()
    }

    // 연결 및 응답 timeout을 설정한다.
    private fun createRequestFactory(timeout: Duration): SimpleClientHttpRequestFactory {
        val requestFactory = SimpleClientHttpRequestFactory()

        // 외부 서버와 연결할 때까지 기다리는 시간
        requestFactory.setConnectTimeout(timeout)

        // 연결 후 응답을 기다리는 시간
        requestFactory.setReadTimeout(timeout)

        return requestFactory
    }

    // 주소가 같아도 정책이 다르면 별도 클라이언트로 관리한다.
    private data class ClientKey(
        val baseUrl: String,
        val policy: ExternalApiCallPolicy,
    )
}
