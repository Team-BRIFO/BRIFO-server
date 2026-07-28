package com.brifo.server

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

fun authenticatedUserId(): UUID {
    val userId = UUID.randomUUID()
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(userId, null, emptyList())
    return userId
}
