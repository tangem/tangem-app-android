package com.tangem.domain.managetokens.repository

import com.tangem.domain.managetokens.model.ManageTokensListBatchFlow
import com.tangem.domain.managetokens.model.ManageTokensListBatchingContext
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

interface ManageTokensRepository {

    fun getTokenListBatchFlow(context: ManageTokensListBatchingContext, batchSize: Int): ManageTokensListBatchFlow

    suspend fun saveManagedCurrencies(
        userWalletId: UserWalletId,
        currenciesToAdd: Map<ManagedCryptoCurrency.Token, Set<Network>>,
        currenciesToRemove: Map<ManagedCryptoCurrency.Token, Set<Network>>,
    )
}