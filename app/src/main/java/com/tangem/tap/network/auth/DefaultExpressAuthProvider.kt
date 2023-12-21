package com.tangem.tap.network.auth

import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.lib.auth.ExpressAuthProvider
import com.tangem.lib.auth.sessionId.ExpressSessionIdGenerator
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

internal class DefaultExpressAuthProvider(
    private val userWalletsStore: UserWalletsStore,
    private val configManager: ConfigManager,
) : ExpressAuthProvider, ExpressSessionIdGenerator {

    private var uuid = AtomicReference(UUID.randomUUID())

    override fun getApiKey(): String {
        return configManager.config.express?.apiKey ?: ""
    }

    override fun getUserId(): String {
        return userWalletsStore.selectedUserWalletOrNull?.walletId?.stringValue ?: ""
    }

    override fun getSessionId(): String {
        return uuid.get().toString()
    }

    override fun generateNewSessionId() {
        uuid = AtomicReference(UUID.randomUUID())
    }
}