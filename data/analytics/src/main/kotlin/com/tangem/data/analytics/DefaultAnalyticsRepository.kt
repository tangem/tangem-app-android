package com.tangem.data.analytics

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectListSync
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.domain.analytics.model.WalletBalanceState
import com.tangem.domain.analytics.repository.AnalyticsRepository
import com.tangem.domain.models.wallet.UserWalletId

internal class DefaultAnalyticsRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : AnalyticsRepository {

    override suspend fun checkIsEventSent(eventId: String): Boolean {
        val sentEvents = appPreferencesStore
            .getObjectListSync<String>(PreferencesKeys.SENT_ONE_TIME_EVENTS_KEY)

        return eventId in sentEvents
    }

    override suspend fun setIsEventSent(eventId: String) {
        appPreferencesStore.editData { mutablePreferences ->
            val sentEvents = mutablePreferences.getObjectList<String>(PreferencesKeys.SENT_ONE_TIME_EVENTS_KEY)
            val updatedSentEvents = sentEvents.orEmpty() + eventId

            mutablePreferences.setObjectList(PreferencesKeys.SENT_ONE_TIME_EVENTS_KEY, updatedSentEvents)
        }
    }

    override suspend fun getWalletBalanceState(userWalletId: UserWalletId): WalletBalanceState? {
        val walletsBalanceState = appPreferencesStore.getObjectMapSync<WalletBalanceState>(
            key = PreferencesKeys.WALLETS_BALANCES_STATES_KEY,
        )

        return walletsBalanceState[userWalletId.stringValue]
    }

    override suspend fun setWalletBalanceState(userWalletId: UserWalletId, balanceState: WalletBalanceState) {
        appPreferencesStore.editData {
            val walletsBalanceState = it.getObjectMap<WalletBalanceState>(
                key = PreferencesKeys.WALLETS_BALANCES_STATES_KEY,
            )
            val updatedWalletsBalanceState = walletsBalanceState
                .plus(pair = userWalletId.stringValue to balanceState)

            it.setObjectMap(PreferencesKeys.WALLETS_BALANCES_STATES_KEY, updatedWalletsBalanceState)
        }
    }
}