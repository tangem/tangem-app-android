package com.tangem.datasource.local.token

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import kotlinx.coroutines.flow.Flow

internal class DefaultStakingActionsStore(
    private val store: RuntimeSharedMapStore<Pair<UserWalletId, CryptoCurrency.ID>, List<StakingAction>>,
) : StakingActionsStore {

    override fun get(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID): Flow<List<StakingAction>> {
        return store.get(key = userWalletId to cryptoCurrencyId)
    }

    override suspend fun store(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
        items: List<StakingAction>,
    ) {
        store.store(key = userWalletId to cryptoCurrencyId, value = items)
    }
}