package com.tangem.tap.network.auth

import com.tangem.lib.auth.ExpressAuthProvider
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

internal class DefaultExpressAuthProvider : ExpressAuthProvider {

    private var uuid = AtomicReference(UUID.randomUUID())

    override fun getSessionId(): String {
        return uuid.get().toString()
    }
}