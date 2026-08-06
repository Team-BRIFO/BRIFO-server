package com.brifo.server.batch

import org.mockito.Mockito
import org.mockito.ArgumentCaptor

@Suppress("UNCHECKED_CAST")
fun <T> anyKotlin(): T {
    Mockito.any<T>()
    return null as T
}

@Suppress("UNCHECKED_CAST")
fun <T> captureKotlin(captor: ArgumentCaptor<T>): T {
    captor.capture()
    return null as T
}
