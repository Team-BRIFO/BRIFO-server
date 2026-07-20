package com.brifo.server.global.external

import org.springframework.web.client.RestClientResponseException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.http.HttpTimeoutException
import java.util.concurrent.TimeoutException

// HTTP 응답을 받은 경우 상태 코드를 반환한다.
internal fun Throwable.responseStatusCode(): Int? {
    return (this as? RestClientResponseException)
        ?.statusCode
        ?.value()
}

// 401 응답인지 확인한다.
internal fun Throwable.isUnauthorized(): Boolean {
    return responseStatusCode() == 401
}

// HTTP 상태 또는 네트워크 오류가 retry 대상인지 확인한다.
internal fun Throwable.isRetryable(): Boolean {
    return responseStatusCode() in RETRYABLE_STATUS_CODES || isRetryableNetworkError()
}

// timeout 또는 일시적인 연결 오류인지 확인한다.
private fun Throwable.isRetryableNetworkError(): Boolean {
    return causes().any {
        it is SocketTimeoutException ||
            it is HttpTimeoutException ||
            it is TimeoutException ||
            it is ConnectException ||
            it is SocketException ||
            it is UnknownHostException
    }
}

// 감싸진 원인 예외까지 순서대로 반환한다.
private fun Throwable.causes(): Sequence<Throwable> {
    return generateSequence(this) { it.cause }
}

private val RETRYABLE_STATUS_CODES = setOf(500, 502, 503, 504)
