package com.tangem.data.settings

import com.tangem.data.source.preferences.PreferencesDataSource
import com.tangem.datasource.local.appcurrency.HiddenBalanceSettingsStore
import com.tangem.domain.balance_hiding.BalanceHidingSettings
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class DefaultSettingsRepository(
    private val preferencesDataSource: PreferencesDataSource,
    private val dispatchers: CoroutineDispatcherProvider,
    private val isBalanceHiddenStore: HiddenBalanceSettingsStore,
) : SettingsRepository {

    override suspend fun isUserAlreadyRateApp(): Boolean {
        return withContext(dispatchers.io) {
            preferencesDataSource.appRatingLaunchObserver.isReadyToShow()
        }
    }

    override suspend fun shouldShowSaveUserWalletScreen(): Boolean {
        return withContext(dispatchers.io) { preferencesDataSource.shouldShowSaveUserWalletScreen }
    }

    override fun isBalanceHiddenEvents(): Flow<BalanceHidingSettings> {
        return isBalanceHiddenStore.get()
    }

    override suspend fun storeBalanceHiddenFlag(balanceHidingSettings: BalanceHidingSettings) {
        isBalanceHiddenStore.store(balanceHidingSettings)
    }

    override suspend fun getBalanceHidingSettings(): BalanceHidingSettings {
        return isBalanceHiddenStore.getSyncOrDefault()
    }
}
