package com.tangem.data.settings

import com.tangem.data.source.preferences.PreferencesDataSource
import com.tangem.datasource.local.appcurrency.BalanceHidingSettingsStore
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class DefaultSettingsRepository(
    private val preferencesDataSource: PreferencesDataSource,
    private val dispatchers: CoroutineDispatcherProvider,
    private val balanceHidingSettingsStore: BalanceHidingSettingsStore,
) : SettingsRepository {

    override suspend fun isUserAlreadyRateApp(): Boolean {
        return withContext(dispatchers.io) {
            preferencesDataSource.appRatingLaunchObserver.isReadyToShow()
        }
    }

    override suspend fun shouldShowSaveUserWalletScreen(): Boolean {
        return withContext(dispatchers.io) { preferencesDataSource.shouldShowSaveUserWalletScreen }
    }

    override fun balanceHidingSettingsEvents(): Flow<BalanceHidingSettings> {
        return balanceHidingSettingsStore.get()
    }

    override suspend fun storeBalanceHidingSettings(balanceHidingSettings: BalanceHidingSettings) {
        balanceHidingSettingsStore.store(balanceHidingSettings)
    }

    override suspend fun getBalanceHidingSettings(): BalanceHidingSettings {
        return balanceHidingSettingsStore.getSyncOrDefault()
    }
}
