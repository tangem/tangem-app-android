package com.tangem.domain.tokens.repository

import com.tangem.domain.models.userwallet.UserWalletId
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.Token

interface NetworksRepository {

    @Suppress("unused") // TODO
    suspend fun getNetworks(ids: Set<Network.ID>): Set<Network>

    @Suppress("unused") // TODO
    suspend fun getStatus(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        networkTokensIds: Set<Token.ID>,
        refresh: Boolean,
    ): NetworkStatus
}
