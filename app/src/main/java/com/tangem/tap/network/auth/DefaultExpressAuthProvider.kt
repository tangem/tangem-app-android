package com.tangem.tap.network.auth

import com.tangem.grow.datasource.express.ExpressAuthProvider
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

internal class DefaultExpressAuthProvider : ExpressAuthProvider {

    private val uuid = AtomicReference(UUID.randomUUID())

    override fun getSessionId(): String {
        return uuid.get().toString()
    }
}