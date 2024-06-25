package com.tangem.datasource.local.token

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

/**
 * Implementation of [UserTokensStore] that based on [appPreferencesStore]
 *
 * @property appPreferencesStore application preference store
 *
 * @author Andrew Khokhlov on 23/06/2024
 */
internal class AppPreferencesUserTokensStore(
    private val appPreferencesStore: AppPreferencesStore,
    private val userTokensStoreMigrationRunner: UserTokensStoreMigrationRunner,
    private val userWalletsStore: UserWalletsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : UserTokensStore {

    init {
        runUserTokensMigrations()
    }

    override fun get(key: UserWalletId): Flow<UserTokensResponse> {
        return appPreferencesStore
            .getObject<UserTokensResponse>(PreferencesKeys.getUserTokensKey(userWalletId = key.stringValue))
            .filterNotNull()
    }

    override suspend fun getSyncOrNull(key: UserWalletId): UserTokensResponse? {
        return appPreferencesStore.getObjectSyncOrNull(
            key = PreferencesKeys.getUserTokensKey(userWalletId = key.stringValue),
        )
    }

    override suspend fun store(key: UserWalletId, value: UserTokensResponse) {
        appPreferencesStore.storeObject(
            key = PreferencesKeys.getUserTokensKey(userWalletId = key.stringValue),
            value = value,
        )
    }

    // TODO: delete in 5.15 (Mobile Sprint 161) https://tangem.atlassian.net/browse/AND-7442
    private fun runUserTokensMigrations() {
        userWalletsStore.userWallets
            .filter { it.isNotEmpty() }
            .take(1)
            .onEach { userWallets ->
                userTokensStoreMigrationRunner.run(ids = userWallets.map { it.walletId.stringValue })
            }
            .flowOn(dispatchers.io)
            .launchIn(CoroutineScope(dispatchers.io))
    }
}
