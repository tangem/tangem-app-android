package com.tangem.data.tokens.repository

import arrow.core.Either
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

internal class DefaultNetworksRepository : NetworksRepository {

    override fun getNetworks(networksIds: Set<Network.ID>): Either<TokensError, Set<Network>> {
        TODO("Not yet implemented")
    }

    override fun getNetworkStatuses(
        userWalletId: UserWalletId,
        networks: Map<Network.ID, Set<Token.ID>>,
        refresh: Boolean,
    ): Flow<Either<TokensError, Set<NetworkStatus>>> {
        TODO("Not yet implemented")
    }
}
