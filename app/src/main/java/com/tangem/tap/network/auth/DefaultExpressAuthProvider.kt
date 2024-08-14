package com.tangem.tap.network.auth

import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.lib.auth.ExpressAuthProvider
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

internal class DefaultExpressAuthProvider(
    private val userWalletsStore: UserWalletsStore,
) : ExpressAuthProvider {

    private var uuid = AtomicReference(UUID.randomUUID())

    override fun getUserId(): String {
        return userWalletsStore.selectedUserWalletOrNull?.walletId?.stringValue ?: error("No user id provided")
    }

    override fun getSessionId(): String {
        return uuid.get().toString()
    }
}