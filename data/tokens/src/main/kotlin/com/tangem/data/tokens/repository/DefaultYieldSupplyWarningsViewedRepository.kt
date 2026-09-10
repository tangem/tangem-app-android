package com.tangem.data.tokens.repository

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.grow.datasource.preferences.GrowPreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSet
import com.tangem.domain.tokens.repository.YieldSupplyWarningsViewedRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

internal class DefaultYieldSupplyWarningsViewedRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val growPreferencesKeys: GrowPreferencesKeys,
    private val dispatchers: CoroutineDispatcherProvider,
) : YieldSupplyWarningsViewedRepository {

    override suspend fun getViewedWarnings(): Set<String> = withContext(dispatchers.io) {
        appPreferencesStore.getObjectSet<String>(growPreferencesKeys.yieldSupplyWarningsStatesKey)
            .firstOrNull().orEmpty()
    }

    override suspend fun view(symbol: String) = withContext(dispatchers.io) {
        appPreferencesStore.editData { mutablePreferences ->
            val stored = mutablePreferences.getObjectSet<String>(
                growPreferencesKeys.yieldSupplyWarningsStatesKey,
            ) ?: mutableSetOf()

            val updated = stored + symbol

            mutablePreferences.setObjectSet<String>(
                key = growPreferencesKeys.yieldSupplyWarningsStatesKey,
                value = updated,
            )
        }
        return@withContext
    }
}