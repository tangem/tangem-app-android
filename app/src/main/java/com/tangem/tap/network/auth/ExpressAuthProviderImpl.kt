package com.tangem.tap.network.auth

import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.lib.auth.ExpressAuthProvider
import com.tangem.lib.auth.sessionId.ExpressSessionIdGenerator
import java.util.UUID

class ExpressAuthProviderImpl(
    private val userWalletsStore: UserWalletsStore,
    private val configManager: ConfigManager,
) : ExpressAuthProvider, ExpressSessionIdGenerator {

    private var uuid = UUID.randomUUID()

    override fun getApiKey(): String {
        return configManager.config.tangemExpressApiKey
    }

    override fun getUserId(): String {
        return userWalletsStore.selectedUserWalletOrNull?.walletId?.stringValue ?: ""
    }

    override fun getSessionId(): String {
        return uuid.toString()
    }

    override fun generateNewSessionId() {
        uuid = UUID.randomUUID()
    }
}
