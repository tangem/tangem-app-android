package com.tangem.data.staking

import com.tangem.datasource.local.token.StakingActionsStore
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.repositories.StakingActionRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

internal class DefaultStakingActionRepository(
    private val stakingActionsStore: StakingActionsStore,
) : StakingActionRepository {

    override suspend fun store(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
        actions: List<StakingAction>,
    ) {
        stakingActionsStore.store(userWalletId, cryptoCurrencyId, actions)
    }

    override fun get(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID): Flow<List<StakingAction>> {
        return stakingActionsStore.get(userWalletId, cryptoCurrencyId)
    }
}
