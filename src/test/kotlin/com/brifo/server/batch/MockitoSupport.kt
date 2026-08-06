package com.brifo.server.batch

import org.mockito.ArgumentCaptor
import org.mockito.Mockito

fun <T> anyKotlin(): T = Mockito.any<T>()

fun <T> captureKotlin(captor: ArgumentCaptor<T>): T = captor.capture()
