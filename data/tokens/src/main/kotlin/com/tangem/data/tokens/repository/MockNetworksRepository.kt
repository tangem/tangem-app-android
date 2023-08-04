package com.tangem.data.tokens.repository

import com.tangem.data.tokens.mock.MockNetworks
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class MockNetworksRepository : NetworksRepository {

    override fun getNetworks(networksIds: Set<Network.ID>): Set<Network> {
        return MockNetworks.networks
            .filter { it.id in networksIds }
            .toSet()
    }

    override fun getNetworkStatuses(
        userWalletId: UserWalletId,
        networks: Map<Network.ID, Set<CryptoCurrency.ID>>,
        refresh: Boolean,
    ): Flow<Set<NetworkStatus>> {
        return flowOf(
            MockNetworks.networksStatuses
                .filter { it.networkId in networks.keys }
                .toSet(),
        )
    }
}
