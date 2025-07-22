package com.tangem.data.staking

import com.tangem.datasource.local.token.StakingActionsStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.repositories.StakingActionRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class DefaultStakingActionRepository(
    private val stakingActionsStore: StakingActionsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : StakingActionRepository {

    override suspend fun store(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
        actions: List<StakingAction>,
    ) {
        withContext(dispatchers.io) {
            stakingActionsStore.store(userWalletId, cryptoCurrencyId, actions)
        }
    }

    override fun get(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID): Flow<List<StakingAction>> {
        return stakingActionsStore.get(userWalletId, cryptoCurrencyId)
    }
}