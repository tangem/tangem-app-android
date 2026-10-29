package com.tangem.data.wallets.balance

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.WalletBalanceState
import com.tangem.domain.wallets.repository.WalletBalanceStateRepository
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import javax.inject.Inject

internal class DefaultWalletBalanceStateRepository @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
    private val storePreferencesKeys: StorePreferencesKeys,
) : WalletBalanceStateRepository {

    override suspend fun getWalletBalanceState(userWalletId: UserWalletId): WalletBalanceState? {
        val walletsBalanceState = appPreferencesStore.getObjectMapSync<WalletBalanceStateDTO>(
            key = storePreferencesKeys.walletsBalancesStatesKey,
        )

        return walletsBalanceState[userWalletId.stringValue]?.let(WalletBalanceStateConverter::convert)
    }

    override suspend fun setWalletBalanceState(userWalletId: UserWalletId, balanceState: WalletBalanceState) {
        appPreferencesStore.editData { preferences ->
            val walletsBalanceState = preferences.getObjectMap<WalletBalanceStateDTO>(
                key = storePreferencesKeys.walletsBalancesStatesKey,
            )
            val updatedWalletsBalanceState = walletsBalanceState
                .plus(pair = userWalletId.stringValue to WalletBalanceStateConverter.convertBack(balanceState))

            preferences.setObjectMap(storePreferencesKeys.walletsBalancesStatesKey, updatedWalletsBalanceState)
        }
    }
}