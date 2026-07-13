package com.brifo.server.global.common

import java.util.UUID

data class CursorPage<out T>(
    val items: List<T>,
    val nextCursor: UUID?,
    val hasNext: Boolean,
)
