package com.tangem.domain.tokens.repository

import arrow.core.Either
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

internal class MockNetworksRepository(
    private val networks: Either<TokensError, Set<Network>>,
    private val statuses: Flow<Either<TokensError, Set<NetworkStatus>>>,
) : NetworksRepository {

    override fun getNetworks(networksIds: Set<Network.ID>): Either<TokensError, Set<Network>> {
        return networks
    }

    override fun getNetworkStatuses(
        userWalletId: UserWalletId,
        networks: Map<Network.ID, Set<Token.ID>>,
        refresh: Boolean,
    ): Flow<Either<TokensError, Set<NetworkStatus>>> {
        return statuses
    }
}