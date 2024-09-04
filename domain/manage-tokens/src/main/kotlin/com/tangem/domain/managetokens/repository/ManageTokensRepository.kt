package com.tangem.domain.managetokens.repository

import com.tangem.domain.managetokens.model.ManageTokensListBatchFlow
import com.tangem.domain.managetokens.model.ManageTokensListBatchingContext
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

interface ManageTokensRepository {

    fun getTokenListBatchFlow(context: ManageTokensListBatchingContext, batchSize: Int): ManageTokensListBatchFlow

    suspend fun hasLinkedTokens(
        userWalletId: UserWalletId,
        network: Network,
        tempAddedTokens: Map<ManagedCryptoCurrency.Token, Set<Network>>,
        tempRemovedTokens: Map<ManagedCryptoCurrency.Token, Set<Network>>,
    ): Boolean
}