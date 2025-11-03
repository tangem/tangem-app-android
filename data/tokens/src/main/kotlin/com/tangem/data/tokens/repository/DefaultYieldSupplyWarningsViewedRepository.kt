package com.tangem.data.tokens.repository

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSet
import com.tangem.domain.tokens.repository.YieldSupplyWarningsViewedRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

internal class DefaultYieldSupplyWarningsViewedRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : YieldSupplyWarningsViewedRepository {

    override suspend fun getViewedWarnings(): Set<String> = withContext(dispatchers.io) {
        appPreferencesStore.getObjectSet<String>(PreferencesKeys.YIELD_SUPPLY_WARNINGS_STATES_KEY).firstOrNull()
            ?: emptySet()
    }

    override suspend fun view(symbol: String) = withContext(dispatchers.io) {
        appPreferencesStore.editData { mutablePreferences ->
            val stored = mutablePreferences.getObjectSet<String>(
                PreferencesKeys.YIELD_SUPPLY_WARNINGS_STATES_KEY,
            ) ?: mutableSetOf()

            val updated = stored + symbol

            mutablePreferences.setObjectSet<String>(
                key = PreferencesKeys.YIELD_SUPPLY_WARNINGS_STATES_KEY,
                value = updated,
            )
        }
        return@withContext
    }
}