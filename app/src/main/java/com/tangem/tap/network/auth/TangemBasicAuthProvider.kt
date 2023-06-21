package com.tangem.tap.network.auth

import com.tangem.lib.auth.BasicAuthProvider

class TangemBasicAuthProvider(private val credentials: String?) : BasicAuthProvider {

    override fun getCredentials(): Map<String, String> = if (credentials == null) {
        mapOf()
    } else {
        mapOf("Authorization" to "Basic $credentials")
    }
}