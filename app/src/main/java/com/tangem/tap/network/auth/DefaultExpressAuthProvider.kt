package com.tangem.tap.network.auth

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.lib.auth.ExpressAuthProvider
import kotlinx.coroutines.runBlocking
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

internal class DefaultExpressAuthProvider(
    private val userWalletsStore: UserWalletsStore,
    private val appPreferencesStore: AppPreferencesStore,
) : ExpressAuthProvider {

    private var uuid = AtomicReference(UUID.randomUUID())

    override fun getUserId(): String {
        return userWalletsStore.selectedUserWalletOrNull?.walletId?.stringValue ?: error("No user id provided")
    }

    override fun getSessionId(): String {
        return uuid.get().toString()
    }

    override fun getRefCode(): String {
        val addedWalletsWithRings = runBlocking {
            appPreferencesStore.getSyncOrDefault(
                key = PreferencesKeys.ADDED_WALLETS_WITH_RING_KEY,
                default = emptySet(),
            )
        }

        val userWalletId = userWalletsStore.selectedUserWalletOrNull?.walletId?.stringValue
            ?: error("No user id provided")

        return if (addedWalletsWithRings.contains(userWalletId)) "ring" else ""
    }
}