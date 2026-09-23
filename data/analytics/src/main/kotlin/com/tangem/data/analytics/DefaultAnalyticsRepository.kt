package com.tangem.data.analytics

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectListSync
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.domain.analytics.model.WalletBalanceState
import com.tangem.domain.analytics.repository.AnalyticsRepository
import com.tangem.domain.models.wallet.UserWalletId

internal class DefaultAnalyticsRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val storePreferencesKeys: StorePreferencesKeys,
) : AnalyticsRepository {

    override suspend fun checkIsEventSent(eventId: String): Boolean {
        val sentEvents = appPreferencesStore
            .getObjectListSync<String>(storePreferencesKeys.sentOneTimeEventsKey)

        return eventId in sentEvents
    }

    override suspend fun setIsEventSent(eventId: String) {
        appPreferencesStore.editData { mutablePreferences ->
            val sentEvents = mutablePreferences.getObjectList<String>(storePreferencesKeys.sentOneTimeEventsKey)
            val updatedSentEvents = sentEvents.orEmpty() + eventId

            mutablePreferences.setObjectList(storePreferencesKeys.sentOneTimeEventsKey, updatedSentEvents)
        }
    }

    override suspend fun getWalletBalanceState(userWalletId: UserWalletId): WalletBalanceState? {
        val walletsBalanceState = appPreferencesStore.getObjectMapSync<WalletBalanceState>(
            key = storePreferencesKeys.walletsBalancesStatesKey,
        )

        return walletsBalanceState[userWalletId.stringValue]
    }

    override suspend fun setWalletBalanceState(userWalletId: UserWalletId, balanceState: WalletBalanceState) {
        appPreferencesStore.editData { preferences ->
            val walletsBalanceState = preferences.getObjectMap<WalletBalanceState>(
                key = storePreferencesKeys.walletsBalancesStatesKey,
            )
            val updatedWalletsBalanceState = walletsBalanceState
                .plus(pair = userWalletId.stringValue to balanceState)

            preferences.setObjectMap(storePreferencesKeys.walletsBalancesStatesKey, updatedWalletsBalanceState)
        }
    }
}