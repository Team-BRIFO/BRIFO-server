package com.brifo.server.batch.common

import org.springframework.batch.infrastructure.item.ItemReader

class IdListItemReader(
    private val ids: List<Long>,
) : ItemReader<Long> {
    private var index = 0

    override fun read(): Long? = ids.getOrNull(index++)
}
