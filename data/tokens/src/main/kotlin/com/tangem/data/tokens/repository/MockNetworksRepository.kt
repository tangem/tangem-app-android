package com.tangem.data.tokens.repository

import arrow.core.Either
import arrow.core.right
import com.tangem.data.tokens.mock.MockNetworks
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class MockNetworksRepository : NetworksRepository {

    override fun getNetworks(networksIds: Set<Network.ID>): Either<TokensError, Set<Network>> {
        return MockNetworks.networks
            .filter { it.id in networksIds }
            .toSet()
            .right()
    }

    override fun getNetworkStatuses(
        userWalletId: UserWalletId,
        networks: Map<Network.ID, Set<Token.ID>>,
        refresh: Boolean,
    ): Flow<Either<TokensError, Set<NetworkStatus>>> {
        return flowOf(
            MockNetworks.networksStatuses
                .filter { it.networkId in networks.keys }
                .toSet()
                .right(),
        )
    }
}
