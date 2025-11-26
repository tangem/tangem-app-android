package com.tangem.domain.staking.repositories

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface StakeKitActionRepository {

    suspend fun store(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID, actions: List<StakingAction>)

    fun get(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID): Flow<List<StakingAction>>
}