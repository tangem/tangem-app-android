package com.tangem.grow.datasource.express

import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

internal class DefaultExpressAuthProvider : ExpressAuthProvider {

    private val uuid = AtomicReference(UUID.randomUUID())

    override fun getSessionId(): String {
        return uuid.get().toString()
    }
}