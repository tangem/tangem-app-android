package com.tangem.grow.datasource.collectibles.models.response.model

import com.tangem.grow.datasource.collectibles.CollectiblesAuthProvider
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

internal class DefaultCollectiblesAuthProvider : CollectiblesAuthProvider {

    private val uuid = AtomicReference(UUID.randomUUID())

    override fun getSessionId(): String {
        return uuid.get().toString()
    }
}