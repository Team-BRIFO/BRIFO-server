package com.brifo.server.externalapi.kis

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * 값을 실제로 보관하는 Valkey 대역.
 *
 * `get()`이 항상 null인 단순 stub을 쓰면 호출마다 새 토큰을 발급하게 되어,
 * 1분 안에 두 번 호출하는 순간 KIS 분당 발급 제한(`EGW00133`)에 걸린다.
 * TTL 만료는 흉내 내지 않는다 — 테스트 구간에서는 만료가 일어나지 않는다.
 */
fun fakeStringRedisTemplate(
    store: ConcurrentHashMap<String, String> = ConcurrentHashMap(),
): StringRedisTemplate {
    val redisTemplate = mock(StringRedisTemplate::class.java)

    @Suppress("UNCHECKED_CAST")
    val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, String>

    `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)

    `when`(valueOperations.get(anyString())).thenAnswer { invocation ->
        store[invocation.getArgument(0)]
    }

    doAnswer { invocation ->
        store[invocation.getArgument<String>(0)] = invocation.getArgument(1)
        null
    }.`when`(valueOperations).set(anyString(), anyString(), any(Duration::class.java))

    `when`(
        valueOperations.setIfAbsent(anyString(), anyString(), any(Duration::class.java)),
    ).thenAnswer { invocation ->
        store.putIfAbsent(invocation.getArgument(0), invocation.getArgument(1)) == null
    }

    `when`(redisTemplate.delete(anyString())).thenAnswer { invocation ->
        store.remove(invocation.getArgument<String>(0)) != null
    }

    return redisTemplate
}
