package com.tangem.datasource.local.token

import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface StakingActionsStore {

    fun get(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID): Flow<List<StakingAction>>

    suspend fun store(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID, items: List<StakingAction>)
}
