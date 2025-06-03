package com.tangem.datasource.local.token

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DefaultStakingActionsStore(
    private val dataStore: StringKeyDataStore<List<StakingAction>>,
) : StakingActionsStore {

    private val mutex = Mutex()

    override fun get(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID): Flow<List<StakingAction>> {
        return dataStore.get(composeKey(userWalletId, cryptoCurrencyId))
    }

    override suspend fun store(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
        items: List<StakingAction>,
    ) {
        mutex.withLock {
            dataStore.store(composeKey(userWalletId, cryptoCurrencyId), items)
        }
    }

    private fun composeKey(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID): String {
        return userWalletId.stringValue + cryptoCurrencyId.value
    }
}