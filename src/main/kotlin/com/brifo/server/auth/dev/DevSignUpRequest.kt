package com.brifo.server.auth.dev

class DevSignUpRequest(
    val password: String,
) {
    override fun toString(): String = "DevSignUpRequest(password=******)"
}
