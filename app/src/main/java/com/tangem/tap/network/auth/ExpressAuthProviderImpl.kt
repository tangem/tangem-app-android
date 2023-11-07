package com.tangem.tap.network.auth

import com.tangem.common.extensions.toHexString
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.lib.auth.AuthProvider
import com.tangem.lib.auth.ExpressAuthProvider
import com.tangem.lib.auth.ExpressSessionIdGenerator
import com.tangem.tap.proxy.AppStateHolder
import java.util.UUID

class ExpressAuthProviderImpl(
    private val walletStateHolder: WalletsStateHolder,
    private val userWalletsStore: UserWalletsStore,
    private val appStateHolder: AppStateHolder
) : ExpressAuthProvider, ExpressSessionIdGenerator {

    private var uuid = UUID.randomUUID()

    override fun getApiKey(): String {
        TODO()
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