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
* [REDACTED_AUTHOR]
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
// [REDACTED_TODO_COMMENT]
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
